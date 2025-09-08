package com.sinse.javawebcrawling.controller;

import com.sinse.javawebcrawling.domain.Product;
import com.sinse.javawebcrawling.service.SimpleCrawlerService;
import com.sinse.javawebcrawling.service.JsonWebCrawlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CrawlingController {
    @Value("${lotte.url}")
    private String lotteUrl;

    private final JsonWebCrawlingService jsonWebCrawlingService;
    private final SimpleCrawlerService simpleCrawlerService;
    @GetMapping("/lotte")
    public List<Product> test()throws IOException {
//        simpleCrawlerService.starePage(lotteWine);
        return simpleCrawlerService.starePage(lotteUrl);
    }
}
