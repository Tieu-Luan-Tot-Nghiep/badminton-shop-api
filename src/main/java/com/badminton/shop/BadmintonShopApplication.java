package com.badminton.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BadmintonShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BadmintonShopApplication.class, args);
    }

}
