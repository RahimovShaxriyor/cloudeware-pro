package com.cloudware.services.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cloudware")
public class WmsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsServiceApplication.class, args);
    }
}
