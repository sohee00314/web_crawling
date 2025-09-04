package com.sinse.javawebcrawling.controller;

import com.sinse.javawebcrawling.service.WebCrawlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class CrawlingController {
    private final WebCrawlingService webCrawlingService;
    @GetMapping("/test")
    public String test()throws IOException {
        webCrawlingService.lotteCrawler();
        return "ok";
    }
}
