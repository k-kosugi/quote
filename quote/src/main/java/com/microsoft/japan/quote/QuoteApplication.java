package com.microsoft.japan.quote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.microsoft.japan.configuration", "com.microsoft.japan.quote", "com.microsoft.japan.catalog"})
public class QuoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuoteApplication.class, args);
	}

}
