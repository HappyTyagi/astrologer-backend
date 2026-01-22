package com.astro.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AstrologerBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(AstrologerBackendApplication.class, args);
	}
}
