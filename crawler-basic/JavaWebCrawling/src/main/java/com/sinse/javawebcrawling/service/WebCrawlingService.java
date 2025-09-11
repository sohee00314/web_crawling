package com.sinse.javawebcrawling.service;

import com.sinse.javawebcrawling.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
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
    public List<Product> crawler(String html) throws IOException {
        //상품을 저장할 리스트
        List<Product> products = new ArrayList<>();
        //크롤링할 웹사이트 html
        Document doc = Jsoup.parse(html);
        //상품정보들이 포함되여 있는 <il>의 class 이름
        Elements productItems = doc.select("li.prod_item.prod_layer > div.prod_main_info");
        if (productItems.isEmpty()) {
            productItems = doc.select("div.prod_main_info");
        }

        //더 이상 찾을 수 없으면 상품 리스트 리턴
        if(productItems.isEmpty()){
            log.debug("No products found");
            return products;
        }
        //상품목록만큼 반복문 적용
        for (Element item : productItems) {
            try {
                Product product = getProduct(item);
                if (!product.getProductName().trim().isEmpty()) {
                    products.add(product);
                }
            } catch (Exception e) {
                log.warn("상품 정보 추출 중 오류: {}", e.getMessage());
            }
        }

        if (products.isEmpty()) {
            log.warn("상품을 찾을 수 없습니다.");
        }

        return products;
    }

    /**
     * 추출한 상품정보 Product에 대입
     * @param item 상품정보가 담겨있는 객체
     * @return product 반환
     */
    private Product getProduct(Element item) {
        Product product = new Product();



        // 1. 상품명 추출 (브랜드 + 상품명)
        String productName = null;
        Element titleElement = item.select("a[name=productName]").first();
        if (titleElement != null) {
            productName = titleElement.text().trim();
        }

        // 2. 상품 이미지 추출
        String imageUrl = null;
        Element imgElement = item.select("div.thumb_image a.thumb_link img").first();
        if (imgElement != null) {
            imageUrl = imgElement.attr("src");
            if (imageUrl != null && imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            }
        }

        // 3. 상세페이지 링크 추출
        String detailLink = null;
        Element linkElement = item.select("a[name=productName]").first();
        if (linkElement != null) {
            detailLink = linkElement.attr("href");
            if (detailLink != null && !detailLink.startsWith("http")) {
                detailLink = "https://prod.danawa.com" + detailLink;
            }
        }

        //카테고리 가져오기
        String category = null;
        Element categoryElement = item.select("div.spec_list").first();
        if (categoryElement != null) {
            for(TextNode textNode : categoryElement.textNodes()) {
                if(!textNode.isBlank()){
                    String all = categoryElement.text().trim();
                    category = all.split("/")[0].trim();
                }
            }
        }


        // Product 객체 설정
        product.setProductName(productName);
        product.setImageUrl(imageUrl);
        product.setDetailLink(detailLink);
        product.setCategory(category);


        return product;
    }

}
