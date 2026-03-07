package com.example.seedbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SeedBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeedBeApplication.class, args);
    }

}
