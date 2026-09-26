package com.example.db2.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.common.ProductJdbcRepository;

/**
 * ตาราง product ใน Database 2
 * SQL ทั้งหมดอยู่ใน ProductJdbcRepository คลาสนี้แค่เลือกว่าจะใช้ JdbcClient ของ db ไหน
 * ต้องใส่ @Qualifier("db2JdbcClient") ให้ตรง db (ถ้าไม่ใส่ Spring จะฉีด db1 ที่เป็น @Primary มาให้)
 * ถ้า db2 ต้องการ SQL เฉพาะ ให้เพิ่ม method ในคลาสนี้ได้เลย
 */
@Repository
public class ProductDb2Repository extends ProductJdbcRepository {

	public ProductDb2Repository(@Qualifier("db2JdbcClient") JdbcClient jdbcClient) {
		super(jdbcClient);
	}
}
