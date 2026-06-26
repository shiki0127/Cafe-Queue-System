package com.cafequeue.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cafequeue")
public class MarketingCouponServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketingCouponServiceApplication.class, args);
    }
}
