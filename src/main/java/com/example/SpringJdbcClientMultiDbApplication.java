package com.example;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.TimeZone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.common.ProductJdbcRepository;
import com.example.db1.repository.ProductDb1Repository;
import com.example.db2.repository.ProductDb2Repository;
import com.example.model.Product;

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
