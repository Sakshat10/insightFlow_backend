package com.insightflow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.insightflow.service.ProjectService;

@SpringBootApplication
public class InsightflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(InsightflowApplication.class, args);
	}

	// @Bean
	// CommandLineRunner run(ProjectService projectService) {
	// 	return args -> {
	// 		projectService.createProject();
	// 	};
	// }
}