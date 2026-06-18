package com.succeshub.succes_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. Component scan covers appdomain services, controllers,
 * and {@link com.succeshub.config.JpaConfig} for JPA repository registration.
 */
@SpringBootApplication(scanBasePackages = {
		"com.succeshub.succes_hub",
		"com.succeshub.config",
		"com.succeshub.coreinfra",
		"com.succeshub.appdomain"
})
public class SuccesHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuccesHubApplication.class, args);
	}

}
