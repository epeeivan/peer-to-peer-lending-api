package com.taf.p2plending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class P2pLendingApplication {

	public static void main(String[] args) {
		SpringApplication.run(P2pLendingApplication.class, args);
	}

}
