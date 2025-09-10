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
    @Value("${website.url}")
    private String url;

    private final JsonWebCrawlingService jsonWebCrawlingService;
    private final SimpleCrawlerService simpleCrawlerService;
    @GetMapping("/crawling")
    public List<Product> test()throws IOException {
        return simpleCrawlerService.starePage(url);
    }
}
