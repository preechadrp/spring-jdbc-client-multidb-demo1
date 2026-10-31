package com.example;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;

import com.example.common.ProductJdbcRepository;
import com.example.db1.repository.ProductDb1Repository;
import com.example.db2.repository.ProductDb2Repository;
import com.example.model.Product;
import com.example.service.ProductTxService;

@SpringBootApplication
public class SpringJdbcClientMultiDbApplication {

	public static void main(String[] args) {
		// Instant <-> DATETIME ใช้ timezone ของ JVM ในการแปลง จึงล็อกเป็นเวลาไทยก่อน Spring start
		// ไม่งั้นผลจะขึ้นกับเครื่องที่รัน (เช่น server/docker ที่เป็น UTC จะเก็บเวลาช้าไป 7 ชั่วโมง)
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bangkok"));
		SpringApplication.run(SpringJdbcClientMultiDbApplication.class, args);
	}

	@Bean
	CommandLineRunner demoDb1(ProductDb1Repository repository) {
		return (args) -> runDemo("db1", repository);
	}

	@Bean
	CommandLineRunner demoDb2(ProductDb2Repository repository) {
		return (args) -> runDemo("db2", repository);
	}

	/** ตัวอย่าง transaction ด้วย PlatformTransactionManager / TransactionTemplate (ดู ProductTxService) */
	@Bean
	CommandLineRunner demoTransaction(ProductTxService txService, ProductDb1Repository db1Repository) {
		return (args) -> {
			System.out.println("\n========== transaction (PlatformTransactionManager) ==========");

			// == 1. commit: insert 2 รายการสำเร็จทั้งคู่ ==
			List<Product> saved = txService.saveAllDb1Manual(List.of(
					new Product(null, "Monitor tx", new BigDecimal("5000.00")),
					new Product(null, "Headset tx", new BigDecimal("2000.00"))));
			System.out.println("Committed: " + saved);

			// == 2. rollback: รายการที่ 2 name = null ผิด NOT NULL -> รายการแรกต้องหายไปด้วย ==
			long countBefore = db1Repository.count();
			try {
				txService.saveAllDb1(List.of(
						new Product(null, "Should be rolled back", new BigDecimal("100.00")),
						new Product(null, null, new BigDecimal("200.00"))));
			} catch (DataAccessException ex) {
				System.out.println("Rolled back: " + ex.getClass().getSimpleName());
			}
			System.out.println("Count before = " + countBefore + ", after = " + db1Repository.count());

			// == 3. setRollbackOnly(): ลดราคาจาก 5000 เหลือ 1000 (ต่ำกว่าครึ่ง) -> ไม่บันทึก ==
			Long monitorId = saved.get(0).id();
			boolean committed = txService.updatePriceDb1(monitorId, new BigDecimal("1000.00"));
			System.out.println("Update committed? " + committed + " -> " + db1Repository.findById(monitorId).orElse(null));

			// == 4. ข้าม 2 database: ย้าย Headset จาก db1 ไป db2 ==
			Product moved = txService.moveDb1ToDb2(saved.get(1).id());
			System.out.println("Moved to db2: " + moved);
		};
	}

	/** ขั้นตอนทดสอบเหมือนกันทั้ง db1 และ db2 ต่างกันแค่ repository ที่ส่งเข้ามา */
	private static void runDemo(String dbName, ProductJdbcRepository repository) {
		System.out.println("\n========== " + dbName + " ==========");

		// == 1. Insert ข้อมูล ==
		// ตอน Insert ให้ส่ง ID เป็น null ไปก่อน เพื่อให้ DB รัน Auto-increment เอง
		// insertDateTime = Instant.now() (constructor 3 ตัวแปรของ Product ใส่ให้)
		Product p1 = new Product(null, "Gaming Mouse " + dbName, new BigDecimal("1500.00"));
		Product p2 = new Product(null, "Mechanical Keyboard " + dbName, new BigDecimal("3500.00"));

		p1 = repository.save(p1); // สังเกตว่า p1 ตัวใหม่จะได้ ID กลับมาจาก DB
		repository.save(p2);
		System.out.println("Inserted Product: " + p1);

		// == 2. Find ข้อมูลทั้งหมด ==
		System.out.println("\n--- All Products ---");
		repository.findAll().forEach(product -> System.out.println(product));

		// == 3. ลองใช้ Custom Query แบบระบุราคา ==
		System.out.println("\n--- Products expensive than 2000 ---");
		repository.findExpensiveProducts(new BigDecimal("2000.00"))
				.forEach(System.out::println);

		// == 4. อัปเดตข้อมูลด้วย SQL ตรงๆ ==
		System.out.println("\n--- Updating Price ---");
		boolean updated = repository.updatePrice(p1.id(), new BigDecimal("1200.00"));
		System.out.println("Update success? : " + updated);

		// ดูผลลัพธ์หลังอัปเดต
		System.out.println("Updated Product: " + repository.findById(p1.id()).orElse(null));

		// == 5. ค้นหาด้วยช่วงเวลา (Instant) ==
		// "วันนี้" ตามเวลาไทย: 00:00 ถึง 00:00 ของวันถัดไป (Asia/Bangkok) แปลงเป็น Instant
		System.out.println("\n--- Products inserted today (Asia/Bangkok) ---");
		LocalDate today = LocalDate.now(Product.BANGKOK);
		Instant from = today.atStartOfDay(Product.BANGKOK).toInstant();
		Instant to = today.plusDays(1).atStartOfDay(Product.BANGKOK).toInstant();
		repository.findByInsertDateTimeBetween(from, to).forEach(System.out::println);

		// ดึงแค่บางฟิลด์เข้า record
		System.out.println("\n--- Select some field to record ---");
		var result = repository.findAllProductSummaries();
		result.forEach(productSummary -> System.out.println(productSummary));
	}

}
