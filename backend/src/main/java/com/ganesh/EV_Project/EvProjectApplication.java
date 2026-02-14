package com.ganesh.EV_Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EvProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvProjectApplication.class, args);
	}
}
