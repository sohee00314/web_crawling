package com.sinse.javawebcrawling.controller;

import com.sinse.javawebcrawling.domain.Product;
import com.sinse.javawebcrawling.service.CrawlerService;
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

    private final CrawlerService crawlerService;
    @GetMapping("/crawling")
    public List<Product> test()throws IOException {
        return crawlerService.starePage(url);
    }
}
