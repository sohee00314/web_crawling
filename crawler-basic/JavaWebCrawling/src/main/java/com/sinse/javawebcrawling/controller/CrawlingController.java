package com.sinse.javawebcrawling.controller;

import com.sinse.javawebcrawling.domain.Product;
import com.sinse.javawebcrawling.service.SimpleCrawlerService;
import com.sinse.javawebcrawling.service.WebCrawlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CrawlingController {
    private final WebCrawlingService webCrawlingService;
    private final SimpleCrawlerService simpleCrawlerService;
    @GetMapping("/lotte")
    public List<Product> test()throws IOException {
        String lotteWine= "https://www.lotteon.com/csearch/render/category?render=nqapi&platform=pc&collection_id=9&login=Y&u9=navigate&u8=FC01220406&mallId=1";
        simpleCrawlerService.starePage(lotteWine);
        return webCrawlingService.lotteCrawler(lotteWine);
    }
}
