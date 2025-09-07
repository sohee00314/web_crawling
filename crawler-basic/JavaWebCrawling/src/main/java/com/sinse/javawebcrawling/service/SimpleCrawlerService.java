package com.sinse.javawebcrawling.service;

import com.sinse.javawebcrawling.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimpleCrawlerService {
    private final WebCrawlingService webCrawlingService;

    @Value("${chrom.driver.path}")
    private String WEB_DRIVER_PATH;

    private WebDriver driver;

    public List<Product> starePage(String url){
        //전체 상품목록
        List<Product> allProducts = new ArrayList<>();
        try {
            System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
            driver = new ChromeDriver();
            driver.get(url);


            while (true) {
                try {
                    String html = driver.getPageSource();
                    List<Product> pageItems = webCrawlingService.lotteCrawler(html);
                    allProducts.addAll(pageItems);
//                    log.info("현재 페이지에서 {}개 상품 수집, 총 {}개", pageItems.size(), allProducts.size());

                    List<WebElement> nextButtons = driver.findElements(By.cssSelector("div.srchPagination a.srchPaginationNext"));
                    if (nextButtons.isEmpty()) {
                        // 다음 페이지 버튼이 없으면 마지막 페이지
                        log.info("마지막 페이지 도달");
                        break;
                    }

                    WebElement next = nextButtons.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", next);
                    Thread.sleep(5000);
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    log.error("스레드 중단");
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(driver!=null){
                driver.quit();
            }
        }
        log.debug("가져온 상품 수 {}",allProducts.size());
        return allProducts;
    }
}
