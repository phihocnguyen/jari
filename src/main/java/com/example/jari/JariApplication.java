package com.example.jari;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching
public class JariApplication {

	public static void main(String[] args) {
		SpringApplication.run(JariApplication.class, args);
	}

}

