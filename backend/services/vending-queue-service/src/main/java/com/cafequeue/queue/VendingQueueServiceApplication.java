package com.cafequeue.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cafequeue")
public class VendingQueueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendingQueueServiceApplication.class, args);
    }
}
