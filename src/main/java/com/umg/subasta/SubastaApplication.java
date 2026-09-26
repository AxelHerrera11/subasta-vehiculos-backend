package com.umg.subasta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubastaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubastaApplication.class, args);
    }
}
