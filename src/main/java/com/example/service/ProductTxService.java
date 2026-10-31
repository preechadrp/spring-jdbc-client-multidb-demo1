package com.example.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.db1.repository.ProductDb1Repository;
import com.example.db2.repository.ProductDb2Repository;
import com.example.model.Product;

/**
 * ตัวอย่างคุม transaction ด้วย PlatformTransactionManager ในโค้ด (programmatic) แทน @Transactional
 *
 * ทำไมใช้ได้: JdbcClient ข้างในใช้ DataSourceUtils.getConnection(dataSource)
 * เมื่อ DataSourceTransactionManager เปิด transaction จะผูก Connection ไว้กับ thread ปัจจุบัน
 * JdbcClient ที่สร้างจาก DataSource ตัวเดียวกันจึงได้ Connection เดิม -> อยู่ใน transaction เดียวกัน
 * (ต้องเป็นคู่เดียวกันเสมอ: db1JdbcClient + db1TransactionManager, db2JdbcClient + db2TransactionManager)
 *
 * มี 2 แบบ
 *  1. PlatformTransactionManager ตรงๆ : getTransaction() -> commit() / rollback() เขียนเอง
 *  2. TransactionTemplate              : ห่อแบบที่ 1 ไว้ให้ ส่งแค่ lambda (แนะนำ เขียนสั้นและพลาดยากกว่า)
 *
 * ข้อดีเทียบกับ @Transactional
 *  - ไม่ใช้ proxy จึงไม่มีปัญหาเรียก method ในคลาสเดียวกันแล้ว transaction ไม่ทำงาน (self-invocation)
 *  - กำหนดขอบเขต transaction ได้แค่บางส่วนของ method
 *  - เลือก transaction manager ตอน runtime ได้ (เช่น db1 หรือ db2)
 */
@Service
public class ProductTxService {

	private final ProductDb1Repository db1Repository;
	private final ProductDb2Repository db2Repository;

	private final PlatformTransactionManager db1TxManager;

	private final TransactionTemplate db1TxTemplate;
	private final TransactionTemplate db1ReadOnlyTxTemplate;
	private final TransactionTemplate db2TxTemplate;

	public ProductTxService(
			ProductDb1Repository db1Repository,
			ProductDb2Repository db2Repository,
			@Qualifier("db1TransactionManager") PlatformTransactionManager db1TxManager,
			@Qualifier("db2TransactionManager") PlatformTransactionManager db2TxManager) {
		this.db1Repository = db1Repository;
		this.db2Repository = db2Repository;
		this.db1TxManager = db1TxManager;

		// TransactionTemplate เป็น thread-safe สร้างครั้งเดียวแล้วใช้ซ้ำได้
		this.db1TxTemplate = new TransactionTemplate(db1TxManager);
		this.db1TxTemplate.setTimeout(10); // วินาที

		// เทียบได้กับ @Transactional(transactionManager = "db1TransactionManager", readOnly = true)
		this.db1ReadOnlyTxTemplate = new TransactionTemplate(db1TxManager);
		this.db1ReadOnlyTxTemplate.setReadOnly(true);

		this.db2TxTemplate = new TransactionTemplate(db2TxManager);
	}

	// =====================================================================
	// แบบที่ 1: ใช้ PlatformTransactionManager ตรงๆ
	// =====================================================================

	/**
	 * insert หลายรายการลง db1 ใน transaction เดียว ถ้ารายการไหนพัง จะ rollback ทั้งหมด
	 * เทียบเท่า @Transactional(transactionManager = "db1TransactionManager")
	 */
	public List<Product> saveAllDb1Manual(List<Product> products) {
		// กำหนดคุณสมบัติของ transaction (เทียบกับ attribute ของ @Transactional)
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("ProductTxService.saveAllDb1Manual");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		def.setTimeout(10);

		// เริ่ม transaction (ยืม Connection จาก pool แล้ว setAutoCommit(false))
		TransactionStatus status = db1TxManager.getTransaction(def);

		List<Product> saved = new ArrayList<>();
		try {
			for (Product product : products) {
				saved.add(db1Repository.save(product));
			}
		} catch (RuntimeException | Error ex) {
			db1TxManager.rollback(status);
			throw ex;
		}
		// commit อยู่นอก try: ถ้า commit เองพัง manager จัดการ rollback ให้แล้ว
		// ไม่ต้อง (และห้าม) เรียก rollback ซ้ำ ไม่งั้นจะได้ IllegalTransactionStateException
		db1TxManager.commit(status);
		return saved;
	}

	// =====================================================================
	// แบบที่ 2: ใช้ TransactionTemplate (แนะนำ)
	// =====================================================================

	/**
	 * เหมือน saveAllDb1Manual แต่สั้นกว่า
	 * lambda จบปกติ -> commit, โยน RuntimeException/Error -> rollback แล้วโยนต่อ
	 */
	public List<Product> saveAllDb1(List<Product> products) {
		return db1TxTemplate.execute(status -> products.stream()
				.map(db1Repository::save)
				.toList());
	}

	/**
	 * rollback โดยไม่ต้องโยน exception ใช้ status.setRollbackOnly()
	 * ตัวอย่าง: ปรับราคา แต่ถ้าราคาใหม่ต่ำกว่าครึ่งของราคาเดิม ให้ยกเลิก
	 *
	 * @return true = commit, false = rollback (หรือไม่พบสินค้า)
	 */
	public boolean updatePriceDb1(Long id, BigDecimal newPrice) {
		return db1TxTemplate.execute(status -> {
			Product before = db1Repository.findById(id).orElse(null);
			if (before == null) {
				return false;
			}

			db1Repository.updatePrice(id, newPrice);

			BigDecimal minAllowed = before.price().divide(BigDecimal.TWO);
			if (newPrice.compareTo(minAllowed) < 0) {
				status.setRollbackOnly(); // จบ lambda แล้วจะ rollback แทน commit
				return false;
			}
			return true;
		});
	}

	/** transaction แบบ read-only (ไม่มีค่าคืน ใช้ executeWithoutResult ก็ได้) */
	public List<Product> findAllDb1() {
		return db1ReadOnlyTxTemplate.execute(status -> db1Repository.findAll());
	}

	// =====================================================================
	// ใช้ 2 database พร้อมกัน
	// =====================================================================

	/**
	 * ย้ายสินค้าจาก db1 ไป db2: ลบจาก db1 + insert ลง db2
	 *
	 * ข้อควรรู้: 2 DataSource = 2 transaction แยกกัน ไม่ใช่ transaction เดียว (ไม่ใช่ XA / 2PC)
	 * เรียงลำดับให้ปลอดภัยที่สุดแทน:
	 *  - transaction นอก = db1 (ลบ) , transaction ใน = db2 (insert) จะ commit ก่อน
	 *  - db2 insert พัง     -> exception ทะลุออกมา db1 rollback ด้วย ข้อมูลไม่หาย
	 *  - db1 commit พังทีหลัง -> สินค้าอยู่ทั้ง 2 ที่ (ซ้ำ) แต่ไม่หาย ตรวจและลบซ้ำทีหลังได้
	 */
	public Product moveDb1ToDb2(Long id) {
		return db1TxTemplate.execute(db1Status -> {
			Product product = db1Repository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("ไม่พบ product id=" + id + " ใน db1"));

			db1Repository.deleteById(id); // ยังไม่ commit

			// คนละ manager จึงเป็น transaction ใหม่ของ db2 ไม่เกี่ยวกับ db1
			Product moved = db2TxTemplate.execute(db2Status -> db2Repository.insert(product.withId(null)));

			return moved; // จบ lambda -> commit db1
		});
	}
}
