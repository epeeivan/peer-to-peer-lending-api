package com.taf.p2plending;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class P2pLendingApplicationTests {

	@Test
	void contextLoads() {
	}

}
