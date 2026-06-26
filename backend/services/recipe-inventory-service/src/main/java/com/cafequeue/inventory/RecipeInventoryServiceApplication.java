package com.cafequeue.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cafequeue")
public class RecipeInventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecipeInventoryServiceApplication.class, args);
    }
}
