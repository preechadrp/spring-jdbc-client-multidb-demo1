package com.example.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.example.dto.ProductSummary;
import com.example.model.Product;

/**
 * SQL ของตาราง product (MariaDB) เขียนด้วย JdbcClient (Spring 6.1+ / Spring Boot 3.2+)
 *
 * ใช้ร่วมกันทั้ง db1 และ db2 ต่างกันแค่ JdbcClient ที่ส่งเข้ามา
 * (ดู ProductDb1Repository / ProductDb2Repository)
 *
 * รูปแบบการเขียน:  jdbcClient.sql(SQL).param(...).query(...).list()/optional()/single()
 *                  jdbcClient.sql(SQL).param(...).update()
 *
 * เทียบกับ Spring Data JDBC (CrudRepository) เดิม
 *  - ต้องเขียน SQL เองทุกคำสั่ง แต่เห็น SQL ชัดเจน ไม่ต้องพึ่ง Dialect / @Table / @Column
 *  - ใช้ชื่อ parameter (:id) แทน ? จึงไม่ต้องนับลำดับ
 *  - optional() คืน Optional เมื่อไม่พบข้อมูล
 */
public abstract class ProductJdbcRepository {

	private static final String SELECT_ALL_COLUMNS = "SELECT id, name, price, insertDateTime FROM product";

	protected final JdbcClient jdbcClient;

	protected ProductJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	// ResultSet.getXxx("name") ไม่สนตัวเล็ก/ใหญ่ ของชื่อคอลัมน์
	protected final RowMapper<Product> rowMapper = (rs, rowNum) -> new Product(
			rs.getLong("id"),
			rs.getString("name"),
			rs.getBigDecimal("price"),
			toInstant(rs.getTimestamp("insertDateTime")));

	/*
	 * แปลง Instant <-> Timestamp สำหรับคอลัมน์ DATETIME ของ MariaDB
	 * DATETIME ไม่เก็บ timezone ไดรเวอร์จึงใช้ timezone ของ JVM (Asia/Bangkok ตั้งใน main)
	 * แปลงไป-กลับ ค่าที่เห็นใน DB จึงเป็นเวลาไทย
	 * ใช้ Timestamp แทนการส่ง Instant ตรงๆ เพื่อไม่ขึ้นกับว่าไดรเวอร์รองรับ Instant หรือไม่
	 */
	protected static Timestamp toTimestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	protected static Instant toInstant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	/**
	 * id == null -> insert แล้วคืน Product ที่มี id จาก DB (auto-increment)
	 * id != null -> update
	 * (พฤติกรรมเดียวกับ CrudRepository.save() เดิม)
	 */
	public Product save(Product product) {
		if (product.id() == null) {
			return insert(product);
		}
		update(product);
		return product;
	}

	public Product insert(Product product) {
		String sql = """
				INSERT INTO product (name, price, insertDateTime)
				VALUES (:name, :price, :insertDateTime)
				""";

		Instant insertDateTime = product.insertDateTime() != null ? product.insertDateTime() : Instant.now();

		// KeyHolder รับค่า id ที่ DB สร้างให้ (AUTO_INCREMENT)
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcClient.sql(sql)
				.param("name", product.name())
				.param("price", product.price())
				.param("insertDateTime", toTimestamp(insertDateTime))
				.update(keyHolder, "id");

		// MariaDB อาจคืน key เป็น BigInteger/Long จึงรับเป็น Number แล้วแปลง
		Long id = keyHolder.getKeyAs(Number.class).longValue();
		return new Product(id, product.name(), product.price(), insertDateTime);
	}

	public int update(Product product) {
		String sql = """
				UPDATE product SET
				  name = :name,
				  price = :price,
				  insertDateTime = :insertDateTime
				WHERE id = :id
				""";

		// ใช้ชื่อ parameter จึงใส่ .param() ลำดับไหนก็ได้
		return jdbcClient.sql(sql)
				.param("id", product.id())
				.param("name", product.name())
				.param("price", product.price())
				.param("insertDateTime", toTimestamp(product.insertDateTime()))
				.update(); // คืนจำนวนแถวที่ถูกแก้ไข
	}

	public List<Product> findAll() {
		return jdbcClient.sql(SELECT_ALL_COLUMNS + " ORDER BY id")
				.query(rowMapper)
				.list(); // หลายแถว -> List (ไม่พบ = list ว่าง)
	}

	/** คืน Optional แทน null หรือ exception */
	public Optional<Product> findById(Long id) {
		return jdbcClient.sql(SELECT_ALL_COLUMNS + " WHERE id = :id")
				.param("id", id)
				.query(rowMapper)
				.optional(); // 0 แถว -> Optional.empty(), 1 แถว -> Optional.of(...)
	}

	/** แทน Query Method findByNameContaining ของ Spring Data */
	public List<Product> findByNameContaining(String keyword) {
		return jdbcClient.sql(SELECT_ALL_COLUMNS + " WHERE name LIKE :keyword ORDER BY id")
				.param("keyword", "%" + keyword + "%")
				.query(rowMapper)
				.list();
	}

	/** ค้นหาช่วงเวลาที่บันทึก ใช้ Instant เป็นเงื่อนไขได้เลย */
	public List<Product> findByInsertDateTimeBetween(Instant from, Instant to) {
		return jdbcClient.sql(SELECT_ALL_COLUMNS
				+ " WHERE insertDateTime >= :from AND insertDateTime < :to ORDER BY id")
				.param("from", toTimestamp(from))
				.param("to", toTimestamp(to))
				.query(rowMapper)
				.list();
	}

	public List<Product> findExpensiveProducts(BigDecimal minPrice) {
		return jdbcClient.sql(SELECT_ALL_COLUMNS + " WHERE price > :minPrice ORDER BY id")
				.param("minPrice", minPrice)
				.query(rowMapper)
				.list();
	}

	public boolean updatePrice(Long id, BigDecimal newPrice) {
		int rows = jdbcClient.sql("UPDATE product SET price = :newPrice WHERE id = :id")
				.param("id", id)
				.param("newPrice", newPrice)
				.update();
		return rows > 0;
	}

	/**
	 * ดึงแค่บางฟิลด์เข้า record
	 * query(ProductSummary.class) จะ map คอลัมน์ id, name เข้า record ให้เอง
	 */
	public List<ProductSummary> findAllProductSummaries() {
		return jdbcClient.sql("SELECT id, name FROM product ORDER BY id")
				.query(ProductSummary.class)
				.list();
	}

	public long count() {
		return jdbcClient.sql("SELECT COUNT(*) FROM product")
				.query(Long.class)
				.single();
	}

	public int deleteById(Long id) {
		return jdbcClient.sql("DELETE FROM product WHERE id = :id")
				.param("id", id)
				.update();
	}
}
