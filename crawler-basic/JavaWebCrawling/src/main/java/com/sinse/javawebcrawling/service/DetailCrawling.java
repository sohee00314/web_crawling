package com.sinse.javawebcrawling.service;

import com.sinse.javawebcrawling.domain.Price;
import com.sinse.javawebcrawling.domain.Product;
import com.sinse.javawebcrawling.domain.Review;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public Product detailPage(Product product,WebDriver driver) {
        Product item = product;
        try {
            //상세페이지 html 얻기
            String html = driver.getPageSource();
            if(html != null){
                //상세페이지 ui 조사
                Document doc = Jsoup.parse(html);

                //가격들을 저장할 리스트
                List<Price> priceList = new ArrayList<>();
                //가격들이 있는 class 조회
                for(Element element : doc.getElementsByClass("li.list-item")) {
                    Price price = new Price();

                    //쇼핑몰 구하기
                    Element shop = element.selectFirst(".box__logo img");
                    if(shop != null) {
                        //쇼핑몰 이름
                        price.setShopName(shop.attr("alt").trim());
                        //쇼핑몰 이이콘
                        price.setShopIcon(shop.attr("abs:src"));
                    }

                    //가격
                    Element priceNum = shop.selectFirst(".box__price .sell-price .text__num");
                    if (priceNum != null) {
                        //숫자만 얻어오기
                        price.setPrice(parseMoney(priceNum.text()));
                    }

                    //배송비
                    Element delivery = shop.selectFirst(".box__delivery");
                    if (delivery != null) {
                        String d = delivery.text().trim();
                        //무료라고 적혀있으면 0, 배송비 끝 '원'제거하고 숫자로 저장
                        int fee = d.contains("무료") ? 0 : parseMoney(d);
                        price.setDeliveryFee(fee);
                    }
                    //구매사이트
                    Element link = shop.selectFirst("a.link__full-cover[href]");
                    if (link != null) {
                        //구매 링크 사이트 저장
                        price.setShopLink(link.attr("abs:href"));
                    }
                    priceList.add(price);
                }
                log.debug("상품 가격목록 {}",priceList);
            }

            //리뷰페이지 넘아기기 반복문 후 파싱
            product.setReviews(getReviews(driver,html));

        }
        catch (Exception e){
            log.error("상품 '{}' 상세 페이지 크롤링 실패: {}", product.getProductName(), e.getMessage());
        }
        return item;
    }

    public List<Review> getReviews(WebDriver driver,String html) {
        List<Review> reviews = new ArrayList<>();
        while (true){
            try {
                //다음페이지로 넘어가는 UI이 조회

                //현재 페이지
                Document doc = Jsoup.parse(html, "https://prod.danawa.com");

                //리뷰 가져오기


                List<WebElement> next = driver.findElements(By
                        .cssSelector("div.page_nav_area .nums_area .page_num.now_page + a.page_num, " +
                                "div.page_nav_area a.nav_edge.nav_edge_next.nav_edge_on"));

                //마지막페이지 도달
                if(next.isEmpty()){
                    log.debug("리뷰 마지막패이지 도달했습니다");
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
        return reviews;
    }

    /**
     * 상품가격 또는 배송비에 적혀있는 숫자만 구하기
     * @param s 숫자가 포함되여 있는 문자열
     * @return 숫자만 반환
     */
    static int parseMoney(String s) {
        String digits = s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
