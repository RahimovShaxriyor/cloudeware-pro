package com.cloudware;

import com.cloudware.repository.DataSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * CloudWare Pro — Main entry point.
 *
 * Package structure:
 *   config/      — CORS and web MVC configuration
 *   controller/  — REST endpoints (thin layer, delegates to services)
 *   service/     — Business logic (AuthService, etc.)
 *   repository/  — Database seeding and schema creation
 *   dto/         — All request/response data transfer objects
 */
@SpringBootApplication
public class CloudWareApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudWareApplication.class, args);
    }

    @Bean
    CommandLineRunner init(DataSeeder seeder) {
        return args -> seeder.seed();
    }
}
