package com.mot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
