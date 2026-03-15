package com.succeshub.succes_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.succeshub.succes_hub",
		"com.succeshub.config",
		"com.succeshub.coreinfra"
})
public class SuccesHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuccesHubApplication.class, args);
	}

}
