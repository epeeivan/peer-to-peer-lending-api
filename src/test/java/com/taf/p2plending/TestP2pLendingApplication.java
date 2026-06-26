package com.taf.p2plending;

import org.springframework.boot.SpringApplication;

public class TestP2pLendingApplication {

	public static void main(String[] args) {
		SpringApplication.from(P2pLendingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
