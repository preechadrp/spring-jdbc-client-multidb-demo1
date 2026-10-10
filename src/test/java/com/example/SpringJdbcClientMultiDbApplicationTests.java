package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// useMainMethod = ALWAYS ให้ test start ผ่าน main() เพื่อให้ TimeZone.setDefault("Asia/Bangkok") ทำงานเหมือนตอนรันจริง
// (ค่า default คือ NEVER = ไม่เรียก main() timezone จะขึ้นกับเครื่องที่รัน test)
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class SpringJdbcClientMultiDbApplicationTests {

	@Test
	void contextLoads() {
		//
	}

}
