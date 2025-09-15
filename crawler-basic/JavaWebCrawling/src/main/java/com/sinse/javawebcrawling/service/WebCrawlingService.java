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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 1. 상품명 추출 (브랜드 + 상품명)과 상품명에 있는 용량과 구성 추출
        String productName = null;
        String volume = null;
        String lineup = null;

        Element titleElement = item.select("a[name=productName]").first();
        if (titleElement != null) {
            productName = titleElement.text().trim();

            //용량과 구성이 있는 Map 호출
            Map<String,String> map = usedName(productName);
            if (map != null) {
                volume = map.get("volume");
                if (volume != null) {
                    product.setVolume(Integer.parseInt(volume));
                }
                lineup = map.get("lineup");
                if (lineup != null) {
                    product.setLineup(lineup);
                }
            }
            log.debug("상품명 {}, 용량 {}, 구성{}", productName, volume, lineup);
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
        int code = 0;
        Element linkElement = item.select("a[name=productName]").first();
        if (linkElement != null) {
            detailLink = linkElement.attr("href");
            if (!detailLink.startsWith("http")) {
                detailLink = "https://prod.danawa.com" + detailLink;

            }
        }
        //상세페이지 안에 있는 상품 코드 가져오기
        Matcher m = Pattern.compile("[?&]pcode=(\\d+)").matcher(detailLink != null ? detailLink : "");
        code = m.find() ? Integer.parseInt(m.group(1)) : 0;



        // Product 객체 설정
        product.setProductName(productName);
        product.setCode(code);
        product.setImageUrl(imageUrl);
        product.setDetailLink(detailLink);
        return product;
    }

    /**
     * 상품명에 포함되여 있는 용량(mL,L)와 구성(2개, 2입)을 파싱하기
     * @param productName 상품명
     * @return  volume(용량), lineup(구성)<br> Map 반환
     */
    public Map<String,String> usedName(String productName){
        Map<String,String> map = new HashMap<>();
        String volume = null;
        String lineup = null;

        //상품명에 있는 ml,l 찾기
        Pattern volumePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[mM][lL]|(\\d+(?:\\.\\d+)?)\\s*[lL]");
        Matcher volumeMatcher = volumePattern.matcher(productName);

        if (volumeMatcher.find()) {
            String volumeStr = volumeMatcher.group(1) != null ? volumeMatcher.group(1) : volumeMatcher.group(2);
            try {
                double volumeDouble = Double.parseDouble(volumeStr);
                // L 단위인 경우 ml로 변환
                if (volumeMatcher.group(0).toLowerCase().contains("l") && !volumeMatcher.group(0).toLowerCase().contains("ml")) {
                    volumeDouble *= 1000; // L를 ml로 변환
                }
                volume = String.valueOf((int) volumeDouble);
            } catch (NumberFormatException e) {
                log.debug("용량 파싱 실패: {}", volumeStr);
            }
            map.put("volume", volume);
        }

        //상품명에서 구성(1개, 1입) 얻어오기
        Pattern lineupPattern = Pattern.compile("(\\d+)\\s*(?:개|입)");
        Matcher lineupMatcher = lineupPattern.matcher(productName);

        if (lineupMatcher.find()) {
            try {
                lineup = lineupMatcher.group(1)+" 개";
                map.put("lineup", lineup);
            } catch (NumberFormatException e) {
                log.debug("개수 파싱 실패: {}", lineupMatcher.group(1));
            }
        }

        return  map;
    }

}
