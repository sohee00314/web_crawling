package com.sinse.javawebcrawling.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Element;
import java.util.List;

/**
 * 상세페이지이지 중 리뷰목록을 자동으로 넘어가기
 */
@Service
@Slf4j
public class DetailCrawling {
    //ChromDriver.exe 위치
    @Value("${chrom.driver.path}")
    private String WEB_DRIVER_PATH;
    //웹브라우저를 프로그래밍적으로 제어하는 인터페이스
    private WebDriver driver;

    public void detailPage(String url){
        try {
            System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.get(url);

            //리뷰페이지 넘아기기 반복문
            while (true){
                try {
                    List<WebElement> next = driver.findElements(By.cssSelector("div.pagination a.active"));
                    if(next.isEmpty()){
                        log.debug("리뷰 마지막패이지 도달");
                        break;
                    }
                    //다음페이지로 넘가기기 클릭
                    WebElement nextReview = next.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextReview);
                    Thread.sleep(3000);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    log.error("스레드 중단");
                }
            }

        }
        catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }finally {
            if(driver!=null){
                driver.quit();
            }
        }
    }
}
