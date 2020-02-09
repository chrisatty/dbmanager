package org.mhcommerce.dbmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties") // sets AWS config and MYSQL password
public class ApplicationTests {

	@Test
	public void contextLoads() {
	}

}
