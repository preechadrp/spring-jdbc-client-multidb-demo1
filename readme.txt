-spring JdbcClient - multi db  (clone มาจาก D:\javaDemo1\spring-data-jdbc-multi-db-mariadb)
-mariadb db
- @Bean CommandLineRunner
-การเขียน multi-database ใน springboot จะปิดการสร้าง datasource อัตโนมัติ และเราต้องจัดการเอง datasource เองทั้งหมด

== สิ่งที่เปลี่ยนจากเวอร์ชัน Spring Data JDBC ==
- pom: spring-boot-starter-data-jdbc -> spring-boot-starter-jdbc
- Db1Config/Db2Config: เหลือแค่ DataSource -> JdbcClient -> TransactionManager
  (ไม่ต้องมี @EnableJdbcRepositories, JdbcAggregateTemplate, DataAccessStrategy, Dialect แล้ว)
- repository: เปลี่ยนจาก interface CrudRepository เป็น class ที่เขียน SQL เองด้วย JdbcClient
  SQL อยู่ที่ common/ProductJdbcRepository ใช้ร่วมกัน db1/db2 เลือก db ด้วย @Qualifier("db1JdbcClient") / ("db2JdbcClient")
  JdbcClient: jdbcClient.sql(SQL).param("name", value).query(rowMapper).list()/optional()/single()
  insert แล้วเอา id กลับด้วย KeyHolder: .update(keyHolder, "id")
- model/Product: ไม่ต้องมี @Table/@Id/@Column แล้ว
- วันเวลาใช้ java.time.Instant แทน LocalDateTime (ใช้คอลัมน์เดิม product.insertDateTime ไม่ต้องแก้ตาราง)
  ตารางดู db/mariadb-product.sql
- timezone Asia/Bangkok
  TimeZone.setDefault(...) ใน main  : แปลง Instant <-> DATETIME ใน DB เป็นเวลาไทย
  @JsonFormat(timezone = "Asia/Bangkok") + spring.jackson.time-zone : JSON แสดงเป็น +07:00
  Product.toString() แสดง insertDateTime เป็นเวลาไทย (Instant.toString() ปกติจะเป็น UTC ลงท้าย Z)
  logback-spring.xml: %d{..., Asia/Bangkok} ให้เวลาใน log เป็นเวลาไทย
