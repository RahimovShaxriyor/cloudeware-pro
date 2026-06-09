package com.cloudware.services.platform;

import com.cloudware.repository.DataSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.cloudware")
public class PlatformServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDatabase(DataSeeder seeder) {
        return args -> seeder.seed();
    }
}
