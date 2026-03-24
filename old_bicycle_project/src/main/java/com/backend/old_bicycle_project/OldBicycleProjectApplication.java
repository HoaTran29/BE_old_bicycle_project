package com.backend.old_bicycle_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OldBicycleProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(OldBicycleProjectApplication.class, args);
	}

}
