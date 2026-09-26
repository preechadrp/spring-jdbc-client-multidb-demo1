package com.example.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Database 2 (Secondary) ไม่ใส่ @Primary
 * repository ต้องระบุ @Qualifier("db2JdbcClient") เสมอ ไม่งั้น Spring จะฉีด db1 (Primary) มาให้
 */
@Configuration
public class Db2Config {

	@Bean(name = "db2DataSource")
	@ConfigurationProperties(prefix = "datasource2")
	HikariDataSource dataSource() {
		return new HikariDataSource();
	}

	@Bean(name = "db2JdbcClient")
	JdbcClient jdbcClient(@Qualifier("db2DataSource") DataSource dataSource) {
		return JdbcClient.create(dataSource);
	}

	@Bean(name = "db2TransactionManager")
	PlatformTransactionManager transactionManager(@Qualifier("db2DataSource") DataSource dataSource) {
		// ใช้คู่กับ @Transactional(transactionManager = "db2TransactionManager")
		return new DataSourceTransactionManager(dataSource);
	}
}
