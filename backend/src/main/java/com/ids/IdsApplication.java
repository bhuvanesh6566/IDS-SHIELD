package com.ids;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdsApplication {
	public static void main(String[] args) {
		SpringApplication.run(IdsApplication.class, args);
	}
}
