package com.example.jari;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@EnableCaching
public class JariApplication {

	public static void main(String[] args) {
		SpringApplication.run(JariApplication.class, args);
	}

}
