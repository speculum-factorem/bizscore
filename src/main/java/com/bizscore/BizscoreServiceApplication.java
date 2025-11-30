package com.bizscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
public class BizscoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BizscoreServiceApplication.class, args);
	}

}
