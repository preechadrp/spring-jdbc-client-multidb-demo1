/*
 * ตาราง product สำหรับ MariaDB (database demo)
 *
 * ใช้ตารางเดิมจากโปรเจกต์ spring-data-jdbc-multi-db-mariadb ได้เลย ไม่ต้องแก้
 * ฝั่ง Java ใช้ java.time.Instant แปลงผ่าน java.sql.Timestamp ด้วย timezone ของ JVM (Asia/Bangkok)
 * ค่าที่เห็นใน DB (DATETIME ไม่เก็บ timezone) จึงเป็นเวลาไทย
 * DATETIME(3) เก็บมิลลิวินาทีด้วย (DATETIME เฉยๆ จะตัดเศษวินาทีทิ้ง)
 */
CREATE TABLE IF NOT EXISTS product (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(250) NOT NULL,
  price           DECIMAL(18,2),
  insertDateTime  DATETIME(3)
);
