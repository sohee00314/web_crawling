package com.sinse.javawebcrawling.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SimpleCrawlerService {
    @Value("${chrom.driver.path}")
    private String WEB_DRIVER_PATH;

    private WebDriver driver;

    public void starePage(String url){
        try {
            System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
            driver = new ChromeDriver();
            driver.get(url);

            while (true) {
                try {
                    WebElement next= driver.findElement(By.className("srchPaginationActive"));
                    next.click();
                    Thread.sleep(10000);
                } catch (NoSuchElementException e) {
                    log.debug("마지막 페이지");
                    break;
                }catch (InterruptedException e){
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

    }
}
