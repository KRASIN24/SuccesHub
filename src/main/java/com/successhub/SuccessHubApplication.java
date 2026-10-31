package com.successhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point. Component scan covers {@code com.successhub}
 * (appdomain, coreinfra).
 */
@SpringBootApplication(scanBasePackages = "com.successhub")
@EnableScheduling
public class SuccessHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuccessHubApplication.class, args);
	}

}
