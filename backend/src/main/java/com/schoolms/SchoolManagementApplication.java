package com.schoolms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SchoolManagementApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(SchoolManagementApplication.class, args);
    }
}
