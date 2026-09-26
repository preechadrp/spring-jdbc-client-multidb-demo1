package com.example.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Database 1 (Primary)
 *
 * เวอร์ชัน JdbcClient ไม่ต้องใช้ @EnableJdbcRepositories / JdbcAggregateTemplate / DataAccessStrategy
 * เหมือน Spring Data JDBC แล้ว เหลือแค่ 3 bean ต่อ 1 database
 *   DataSource -> JdbcClient -> TransactionManager
 * แล้ว repository เลือกใช้ด้วย @Qualifier("db1JdbcClient")
 */
@Configuration
public class Db1Config {

	@Primary /*ต้องใส่ตัวแรกเสมอ*/
	@Bean(name = "db1DataSource")
	@ConfigurationProperties(prefix = "datasource1")
	HikariDataSource dataSource() {
		return new HikariDataSource();
	}

	@Primary /*ต้องใส่ตัวแรกเสมอ*/
	@Bean(name = "db1JdbcClient")
	JdbcClient jdbcClient(@Qualifier("db1DataSource") DataSource dataSource) {
		// JdbcClient.create(dataSource) ข้างในสร้าง NamedParameterJdbcTemplate ให้เอง
		return JdbcClient.create(dataSource);
	}

	@Primary /*ต้องใส่ตัวแรกเสมอ*/
	@Bean(name = "db1TransactionManager")
	PlatformTransactionManager transactionManager(@Qualifier("db1DataSource") DataSource dataSource) {
		// ใช้คู่กับ @Transactional(transactionManager = "db1TransactionManager")
		return new DataSourceTransactionManager(dataSource);
	}
}
