package com.example.db1.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.common.ProductJdbcRepository;

/**
 * ตาราง product ใน Database 1
 * SQL ทั้งหมดอยู่ใน ProductJdbcRepository คลาสนี้แค่เลือกว่าจะใช้ JdbcClient ของ db ไหน
 * ต้องใส่ @Qualifier("db1JdbcClient") ให้ตรง db (ถ้าไม่ใส่ Spring จะฉีด db1 ที่เป็น @Primary มาให้)
 * ถ้า db1 ต้องการ SQL เฉพาะ ให้เพิ่ม method ในคลาสนี้ได้เลย
 */
@Repository
public class ProductDb1Repository extends ProductJdbcRepository {

	public ProductDb1Repository(@Qualifier("db1JdbcClient") JdbcClient jdbcClient) {
		super(jdbcClient);
	}
}
