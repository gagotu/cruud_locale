package com.exprivia.nest.cruud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main class of project
 */
@SpringBootApplication(
		scanBasePackages = {"com.exprivia.nest.cruud"}
)
@EnableScheduling
public class CruudApplication {

	/**
	 * Entry point for microservice
	 * @param args from batch
	 */
	public static void main(String[] args) {
		SpringApplication.run(CruudApplication.class, args);
	}

}
