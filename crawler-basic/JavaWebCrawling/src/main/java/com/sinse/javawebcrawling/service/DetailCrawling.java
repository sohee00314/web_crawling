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
                Document doc = Jsoup.parse(html, "https://prod.danawa.com");

                //상품 카테고리 및 종류 가져오기
                Element root =
                        doc.selectFirst("table:has(th:matchesOwn(^\\s*주종\\s*$)), " +   // '주종' th를 가진 테이블
                                "#infoBottom, #productSpec, .prod_spec, .detail_info"); // 페이지별 ID/클래스
                String category = null;
                String kind     = null;
                if(root != null){
                    // "양주 종류 " 아래에 있는 <td> 찾기
                    Element tdKindFirst = root.selectFirst(
                            "tr > th.tit:matchesOwn(^\\s*양주\\s*종류\\s*$) + td.dsc"
                    );
                    //첫번째 <td> 찾기
                    if (tdKindFirst != null) {
                        //상품 종류 파싱(위스키, 레드와인 등)
                        kind = tdKindFirst.text().trim();
                        if (kind.isEmpty()) kind = null;
                    }

                    //주종 찾기
                    Element tdCategory = root.selectFirst(
                            "tr:has(> th.tit:matchesOwn(^\\s*주종\\s*$)) > td.dsc"
                    );
                    //상품 주종 파싱
                    if (tdCategory != null) {
                        category = tdCategory.text().trim();
                    }
                }
                item.setCategory(category);
                item.setProductKind(kind);


                //가격들을 저장할 리스트
                List<Price> priceList = new ArrayList<>();
                //가격들이 있는 class 조회
                for(Element element : doc.select("#lowPriceCompanyArea ul.list__mall-price li.list-item")) {
                    Price price = new Price();

                    //쇼핑몰 구하기
                    Element logoImg = element.selectFirst(".box__logo img.image[alt]");
                    if (logoImg != null) {
                        price.setShopName(logoImg.attr("alt").trim());
                        String icon = logoImg.attr("abs:src");
                        if (icon.isEmpty()) {
                            icon = logoImg.attr("src");
                            if (icon.startsWith("//")) icon = "https:" + icon;
                        }
                        price.setShopIcon(icon);
                    } else {
                        //텍스트 로고 처리(술픽 등)
                        Element textLogo = element.selectFirst(".box__logo .text__logo");
                        if (textLogo != null) {
                            String name = textLogo.hasAttr("aria-label")
                                    ? textLogo.attr("aria-label").trim()
                                    : textLogo.text().trim();
                            price.setShopName(name);
                        }
                        // 아이콘은 없음(null 허용)
                    }


                    //가격
                    Element priceNum = element.selectFirst(".box__price .sell-price .text__num");
                    if (priceNum != null) {
                        //숫자만 얻어오기
                        price.setPrice(parseNum(priceNum.text()));
                    }

                    //배송비
                    Element delivery = element.selectFirst(".box__delivery");
                    if (delivery != null) {
                        String d = delivery.text().trim();
                        //무료라고 적혀있으면 0, 배송비 끝 '원'제거하고 숫자로 저장
                        int fee = d.contains("무료") ? 0 : parseNum(d);
                        price.setDeliveryFee(fee);
                    }
                    //구매사이트
                    Element link = element.selectFirst("a.link__full-cover[href]");
                    if (link != null) {
                        //구매 링크 사이트 저장
                        price.setShopLink(link.attr("abs:href"));
                    }
                    priceList.add(price);
                }
                item.setPrices(priceList);
            }

            //리뷰페이지 넘아기기 반복문 후 파싱
            item.setReviews(getReviews(driver,html));


        }
        catch (Exception e){
            log.error("상품 '{}' 상세 페이지 크롤링 실패: {}", product.getProductName(), e.getMessage());
        }
        return item;
    }

    /**
     * 상품 리뷰 데이터 구하기
     * @param driver 상품목록 때 사용한 driver 재사용
     * @param html 상품상세페이지 html
     * @return 리부리스트 반환
     */
    public List<Review> getReviews(WebDriver driver,String html) {
        List<Review> reviews = new ArrayList<>();
        while (true){
            try {
                //다음페이지로 넘어가는 UI이 조회

                //현재 페이지
                Document doc = Jsoup.parse(html, "https://prod.danawa.com");

                //리뷰 가져오기
                for (Element li : doc.select("li.danawa-prodBlog-companyReview-clazz-more")){
                    Review review = new Review();
                    //작성자
                    Element nameEl = li.selectFirst(".top_info .name");
                    if (nameEl != null) review.setReviewer(nameEl.text().trim());

                    //작성일
                    Element dateEl = li.selectFirst(".top_info .date");
                    if (dateEl != null) review.setReviewDate(dateEl.text().trim());

                    //별점
                    int star = 0;
                    Element starEl = li.selectFirst(".top_info .star_mask");
                    if (starEl != null) {
                        String t = starEl.text();//점수 예) "100점"
                        if (!t.isEmpty()) {
                            star = parseNum(t);
                        } else {
                            String w = starEl.attr("style");// style에 있는 width 예) "width:100%"
                            if (!w.isEmpty()) star = parseNum(w);
                        }
                    }
                    review.setStar(star);

                    //쇼핑물명
                    String shopName = null;
                    //1차로 저회
                    Element mallImg = li.selectFirst(".top_info .mall img[alt]");
                    if (mallImg != null && !mallImg.attr("alt").isBlank()) {
                        shopName = mallImg.attr("alt").trim();
                    }
                    if (shopName == null) {
                        //2차 조회
                        Element mallSpan = li.selectFirst(".top_info .mall span");
                        if (mallSpan != null) shopName = mallSpan.text().trim();
                    }
                    review.setShopName(shopName);

                    //리뷰제목
                    Element titleEl = li.selectFirst(".rvw_atc .tit_W .tit");
                    if (titleEl != null) review.setTitle(titleEl.text().trim());

                    //내용
                    Element contentEl = li.selectFirst(".rvw_atc .atc_cont .atc");
                    if (contentEl != null) review.setContent(contentEl.text().trim());

                    // 사진들
                    for (Element img : li.select(".pto_thumb img, .pto_list img")) {
                        String src = img.attr("abs:src");
                        if (!src.isBlank()) review.getPhotos().add(src);
                    }

                    reviews.add(review);
                }


//                List<WebElement> next = driver.findElements(By
//                        .cssSelector(
//                                "div.page_nav_area .nums_area .page_num.now_page + a.page_num, "
//                                        + "div.page_nav_area a.nav_edge.nav_edge_next.nav_edge_on"));

                //테스트용
                List<WebElement> next = driver.findElements(By
                        .cssSelector(
                                "div.page_nav_area .nums_area .page_num.now_page + a.page_num"));


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

    public Product getCategory(Document doc){
        return null;
    }

    /**
     * 문자열에 포함되여 있는 숫자를 반환하기
     * @param s 숫자가 포함되여 있는 문자열
     * @return 숫자만 반환
     */
    static int parseNum(String s) {
        String digits = s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

}
