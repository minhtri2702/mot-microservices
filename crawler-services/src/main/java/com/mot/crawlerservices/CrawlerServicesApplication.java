package com.mot.crawlerservices;

import com.mot.crawlerservices.crawler.SeleniumCrawler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CrawlerServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerServicesApplication.class, args);
    }


}
