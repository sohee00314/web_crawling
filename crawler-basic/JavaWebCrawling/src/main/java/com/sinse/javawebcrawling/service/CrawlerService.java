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

/**
 * 다음페이지로 동적으로 자동으로 넘어가는 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlerService {
    private final WebCrawlingService webCrawlingService;
    private final DetailCrawling detailCrawling;

    //ChromDriver.exe 위치
    @Value("${chrom.driver.path}")
    private String WEB_DRIVER_PATH;
    //웹브라우저를 프로그래밍적으로 제어하는 인터페이스
    private WebDriver driver;

    /**
     * 자동으로 페이지를 넘어가게 하는 서비스
     * @param url 상품품목록 링크
     * @return 전체 상품 목록 반환
     */
    public List<Product> starePage(String url){
        //전체 페이지에 있는 상품들을 저장한 리스트
        List<Product> allProducts = new ArrayList<>();
        try {
            System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
            driver = new ChromeDriver();
            driver.get(url);


            while (true) {
                try {
                    //상품목록 html
                    String html = driver.getPageSource();
                    //각 페이지의 전체 상품을 담은 리스트 가져오기 가져오기
                    List<Product> pageItems = webCrawlingService.lotteCrawler(html);

//                    for (Product product : pageItems) {
//                        detailCrawling.detailPage(product.getDetailLink());
//                    } 상세페이지

                    //최종 상품리스트에 담기
                    allProducts.addAll(pageItems);
                    log.info("현재 페이지에서 {}개 상품 수집, 총 {}개", pageItems.size(), allProducts.size());

                    //다음 페이지로 넘어가는 ui가져오기
                    List<WebElement> nextButtons = driver.findElements(By.cssSelector("div.num_nav_wrap a.num.now_on + a.num"));
                    if (nextButtons.isEmpty()) {
                        // 다음 페이지 버튼이 없으면 마지막 페이지
                        log.info("마지막 페이지 도달");
                        break;
                    }
                    //다음페이지로 넘가기기 클릭
                    WebElement next = nextButtons.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", next);
                    Thread.sleep(3000);
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    log.error("스레드 중단");
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(driver!=null){
                //드라이버 닫기
                driver.quit();
            }
        }
        log.debug("가져온 상품 수 {}",allProducts.size());
        return allProducts;
    }
}
