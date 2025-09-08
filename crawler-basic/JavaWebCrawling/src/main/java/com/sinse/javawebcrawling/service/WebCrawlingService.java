package com.sinse.javawebcrawling.service;

import com.sinse.javawebcrawling.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * html에서 원하는 데이터를 가져오는 서비스
 */
@Service
@Slf4j
public class WebCrawlingService {
    /**
     * Proudct에서 정의한 데이테 List에 저장
      * @param html 웹사이트 html
     * @return List 반환
     * @throws IOException 에러 정의
     */
    public List<Product> lotteCrawler(String html) throws IOException {
        //상품을 저장할 리스트
        List<Product> products = new ArrayList<>();
        //크롤링할 웹사이트 html
        Document doc = Jsoup.parse(html);
        //상품정보들이 포함되여 있는 <il>의 class 이름
        Elements productItems = doc.select(".s-goods-grid__item .s-goods");
//        if (productItems.isEmpty()) {
            // 다른 가능한 셀렉터들도 시도
//            String[] alternativeSelectors = {
//                    ".s-goods",
//                    ".prd-item",
//                    ".goods-item",
//                    "[data-object*='pd_id']"
//            };

//            for (String selector : alternativeSelectors) {
//                productItems = doc.select(selector);
//                if (!productItems.isEmpty()) {
//                    log.info("대체 셀렉터 '{}'로 {}개 상품 요소 발견", selector, productItems.size());
//                    break;
//                }
//            }
//        }

        for (Element item : productItems) {
            try {
                Product product = extractLotteProduct(item);
                if (product != null && product.getProductName() != null && !product.getProductName().trim().isEmpty()) {
//            log.debug("상품 추출 성공 - 마트: {}, 브랜드: {}, 상품명: {}, 원가: {}, 할인가: {}",
//                    product.getMarket(), product.getBrand(),product.getProductName(),product.getPrice(),product.getDiscount());
                    products.add(product);
                }
            } catch (Exception e) {
                log.warn("상품 정보 추출 중 오류: {}", e.getMessage());
            }
        }

        if (products.isEmpty()) {
            log.warn("롯데온 상품을 찾을 수 없습니다. HTML 구조를 확인하세요.");
            logLotteHtmlStructure(doc);
        }

        return products;
    }

    // 롯데온 상품 개별 추출 로직
    private Product extractLotteProduct(Element item) {
        Product product = new Product();

        // 1. 마트명 추출
        String market = "롯데온"; // 기본값
        Element marketElement = item.select(".s-goods-flag__spot").first();
        if (marketElement != null && !marketElement.text().trim().isEmpty()) {
            market = marketElement.text().trim();
        }

        // 2. 브랜드 추출
        String brand = null;
        Element brandElement = item.select(".s-goods-title__brand").first();
        if (brandElement != null) {
            brand = brandElement.text().trim();
        }

        // 3. 상품명 추출 (브랜드 + 상품명)
        String productName = null;
        Element titleElement = item.select(".s-goods-title").first();
        if (titleElement != null) {
            productName = titleElement.text().trim();
        }

        // 4. 상품 이미지 추출
        String imageUrl = null;
        Element imgElement = item.select(".s-goods-image img").first();
        if (imgElement != null) {
            imageUrl = imgElement.attr("src");
            if (imageUrl != null && imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            }
        }

        // 5. 상세페이지 링크 추출
        String detailLink = null;
        Element linkElement = item.select(".s-goods__anchor").first();
        if (linkElement != null) {
            detailLink = linkElement.attr("href");
            if (detailLink != null && !detailLink.startsWith("http")) {
                detailLink = "https://www.lotteon.com" + detailLink;
            }
        }

        // 6. 원가 추출
        Integer originalPrice = null;
        Element originalPriceElement = item.select(".s-goods-price__original .s-goods-price__number").first();
        if (originalPriceElement != null) {
            String priceText = originalPriceElement.text().replaceAll("[^0-9]", "");
            if (!priceText.isEmpty()) {
                try {
                    originalPrice = Integer.parseInt(priceText);
                } catch (NumberFormatException e) {
                    log.debug("원가 파싱 실패: {}", originalPriceElement.text());
                }
            }
        }

        // 7. 할인가 (최종가격) 추출
        Integer finalPrice = null;
        Element finalPriceElement = item.select(".s-goods-price__final .s-goods-price__number").first();
        if (finalPriceElement != null) {
            String priceText = finalPriceElement.text().replaceAll("[^0-9]", "");
            if (!priceText.isEmpty()) {
                try {
                    finalPrice = Integer.parseInt(priceText);
                } catch (NumberFormatException e) {
                    log.debug("할인가 파싱 실패: {}", finalPriceElement.text());
                }
            }
        }

        // 할인가가 없으면 원가를 사용
        Integer price = finalPrice != null ? finalPrice : originalPrice;

        // Product 객체 설정
        product.setProductName(productName);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setDetailLink(detailLink);
        product.setMarket(market);
        product.setBrand(brand);
        product.setCategory(null); //상세페이지에게 가져올 내용

        // 추출된 정보 로그 (첫 번째 상품만)
        if (product.getProductName() != null) {
        }
        return product;
    }

    // 디버깅용 HTML 구조 분석
    private void logLotteHtmlStructure(Document doc) {
        log.info("=== 롯데온 HTML 구조 분석 ===");

        // 상품 관련 클래스 찾기
        String[] searchClasses = {
                "s-goods", "s-goods-grid", "prd-item", "goods-item",
                "product", "item", "list"
        };

        for (String className : searchClasses) {
            Elements elements = doc.select("." + className);
            if (!elements.isEmpty()) {
                log.info("클래스 '.{}' 요소 {}개 발견", className, elements.size());
            }
        }

        // 가격 관련 요소 찾기
        Elements priceElements = doc.select("*:containsOwn(원)");
        log.info("'원'이 포함된 요소 {}개 발견", priceElements.size());

        // 이미지 요소 찾기
        Elements images = doc.select("img[src]");
        log.info("이미지 요소 {}개 발견", images.size());

        // 링크 요소 찾기
        Elements links = doc.select("a[href]");
        log.info("링크 요소 {}개 발견", links.size());
    }
}
