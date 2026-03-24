package com.mot.crawlerservices.controller;

import com.mot.crawlerservices.crawler.SeleniumCrawler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrawlerController {
    private final SeleniumCrawler seleniumCrawler ;
    @GetMapping("/test")
    public String test() {
        seleniumCrawler.crawlSeries();
        return null ;
    }

}
