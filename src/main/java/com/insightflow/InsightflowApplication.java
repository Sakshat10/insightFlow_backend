package com.insightflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class InsightflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(InsightflowApplication.class, args);
	}
}
