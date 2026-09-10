package com.quickseat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QuickSeatApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickSeatApplication.class, args);
    }
}
