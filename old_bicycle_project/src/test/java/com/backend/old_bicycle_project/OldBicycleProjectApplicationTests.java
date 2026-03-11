package com.backend.old_bicycle_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@ActiveProfiles("test")
class OldBicycleProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
