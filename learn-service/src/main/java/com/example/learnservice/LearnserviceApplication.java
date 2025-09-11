package com.example.learnservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearnserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LearnserviceApplication.class, args);
	}

}