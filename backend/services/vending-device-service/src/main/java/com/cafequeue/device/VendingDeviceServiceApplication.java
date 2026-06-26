package com.cafequeue.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cafequeue")
public class VendingDeviceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendingDeviceServiceApplication.class, args);
    }
}
