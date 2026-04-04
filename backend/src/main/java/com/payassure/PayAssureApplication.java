package com.payassure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayAssureApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayAssureApplication.class, args);
    }
}
