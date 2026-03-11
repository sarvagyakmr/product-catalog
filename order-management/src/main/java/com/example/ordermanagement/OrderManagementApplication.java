package com.example.ordermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.ordermanagement", "com.example.commons"})
@EnableJdbcRepositories(basePackages = {"com.example.ordermanagement.repository", "com.example.commons.repository"})
public class OrderManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagementApplication.class, args);
    }
}
