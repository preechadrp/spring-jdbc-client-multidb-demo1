package com.example.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ตาราง product (id, name, price, insertDateTime)
 *
 * เวอร์ชัน JdbcClient ไม่ต้องใช้ @Table / @Id / @Column ของ Spring Data แล้ว
 * เพราะเราเขียน SQL และ map คอลัมน์เองใน repository
 */
public record Product(
		Long id,
		String name,
		BigDecimal price,

		/*
		 * Instant = จุดเวลาที่แน่นอนบนเส้นเวลา (ไม่ผูกกับ timezone ใด) ต่างจาก LocalDateTime เดิมที่เป็นแค่
		 * "วันที่+เวลาบนนาฬิกา" โดยไม่รู้ว่าเป็นเวลาของประเทศไหน
		 * JSON ต้องมี offset เสมอ เช่น 2026-09-26T18:45:30.085+07:00 (ส่งเข้าหรือแสดงผล)
		 * timezone = "Asia/Bangkok" ทำให้แสดงผลเป็นเวลาไทย
		 */
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Bangkok")
		Instant insertDateTime) {

	public static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

	private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(BANGKOK);

	/** constructor สำหรับสร้างข้อมูลใหม่ (insertDateTime = เวลาปัจจุบัน) */
	public Product(Long id, String name, BigDecimal price) {
		this(id, name, price, Instant.now());
	}

	// สร้าง Wither Method สำหรับอัปเดตข้อมูล (เนื่องจาก Record เป็น Immutable)
	public Product withId(Long id) {
		return new Product(id, this.name, this.price, this.insertDateTime);
	}

	public Product withPrice(BigDecimal price) {
		return new Product(this.id, this.name, price, this.insertDateTime);
	}

	/*
	 * Instant.toString() จะแสดงเป็น UTC (ลงท้ายด้วย Z) เช่น 2026-09-26T11:45:00Z
	 * override toString ให้แสดงเป็นเวลาไทยแทน เช่น 2026-09-26T18:45:00.000+07:00
	 */
	@Override
	public String toString() {
		return "Product[id=" + id + ", name=" + name + ", price=" + price
				+ ", insertDateTime=" + (insertDateTime == null ? null : DISPLAY_FORMAT.format(insertDateTime)) + "]";
	}
}
