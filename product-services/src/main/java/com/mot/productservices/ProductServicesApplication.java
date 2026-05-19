package com.mot.productservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.mot.productservices",
        "com.mot.exception.handler",
        "com.mot.response"
})
@EntityScan(basePackages = "com.mot.productservices.entity")
@EnableJpaRepositories(basePackages = "com.mot.productservices.repository")
public class ProductServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServicesApplication.class, args);
    }
}
