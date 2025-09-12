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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                //상품정보 가져오기
                item.setContent(isContent(doc));

                //상품의 도수, 포장상태 가져오기
                Map<String,String> map = isMap(doc);
                if(map != null){
                    String packaging = map.get("packaging");
                    item.setPackaging(packaging);

                    String alcohol = map.get("alcohol");
                    if(alcohol == null){
                        item.setAlcohol(0);
                    } else {item.setAlcohol(Integer.parseInt(alcohol));}

                }
                log.debug("상품명 {}, 포장상태 {}. 도수 {}도",item.getProductName(),item.getPackaging(),item.getAlcohol());

                //상품 카테고리 및 종류 ,정보 가져오기
                String category = getCategory(doc).get(0);
                String kind= getCategory(doc).get(1);
                item.setCategory(category);
                item.setProductKind(kind);

                //가격들을 저장할 리스트
                List<Price> priceList = getPrices(doc);
                item.setPrices(priceList);
            }

            //리뷰페이지 넘아기기 반복문 후 파싱
//            item.setReviews(getReviews(driver,html));


        }
        catch (Exception e){
            log.error("상품 '{}' 상세 페이지 크롤링 실패: {}", product.getProductName(), e.getMessage());
        }
        return item;
    }

    /**
     * 상품 리뷰 데이터 구하기<br>
     *
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

                }

                //다음 리뷰페이지 전환 UI 모음
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

    /**
     * 상품 주종과 종류를 파싱하기
     * @param doc 파싱을 html
     * @return index(0)=category<br>
     * index(1)=kind<br>
     * index(2)=content
     * 인 String 리스트 반환
     */
    public List<String> getCategory(Document doc){
        List<String> ck = new ArrayList<>();
        Element root =
                doc.selectFirst("table:has(th:matchesOwn(^\\s*주종\\s*$)), " +   // '주종' th를 가진 테이블
                        "#infoBottom, #productSpec, .prod_spec, .detail_info"); // 페이지별 ID/클래스
        String category = null;
        String kind     = null;
        if(root != null){
            // "...종류 " 아래에 있는 <td> 찾기
            Element tdKindFirst = root.selectFirst(
                    "tr > th.tit:matchesOwn(종\\s*류\\s*(?:[:：])?\\s*$) + td.dsc"
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


        ck.add(category); // index(0)
        ck.add(kind); // index(1)
        return ck;
    }

    /**
     * 상품 가격 리스트 구하기<br>
     * 쇼핑물, 쇼핑몰 아이콘, 가격, 배송비, 구매링크
     * @param doc 파싱할 html
     * @return 상품 가격 리스트 반환
     */
    public List<Price> getPrices(Document doc) {
        List<Price> prices = new ArrayList<>();
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
            prices.add(price);
        }

        return  prices;
    }

    /**
     * 상품정보 구하기
     * @param doc 파싱 할 html 정보
     * @return String content 반환
     */
    public String isContent(Document doc) {
        String content = null;
        //가져온 html를 텍스트화
        String fullText = doc.text();
        //제품설명 앞뒤 공백 및 허용하고 한글이 아닌 글자 제거(그릅1)하고 각 줄에 최소 한 글자 이상의 한글이 포함된 열 구하기(그릅2)
        Pattern pattern = java.util.regex.Pattern.compile(
                "제품\\s*설명\\s*([^가-힣]*?)([가-힣][^\\n]*(?:\\n[^가-힣\\n]*[가-힣][^\\n]*)*)",
                Pattern.MULTILINE | Pattern.DOTALL
        );

        //특정 문자열에 검색기 초기화
        Matcher matcher = pattern.matcher(fullText);

        if (matcher.find()) {
            //그룸2의 문자열에서 공백 제거 얻기
            String extracted = matcher.group(2).trim();
            //수상|인증|제조사|본 콘텐츠 앞부분 문자까지 구하기
            extracted = extracted.split("(?=수상|인증|제조사|본 콘텐츠)")[0].trim();
            if (extracted.length() > 10) {
                //불필요한 공백 제거
                content = extracted.replaceAll("\\s{2,}", " ");
            }
        }
        return content;
    }

    /**
     * div.spec_list 안에 있는 정보 파싱하기<br>
     * index(0)= 포장상태 <br>
     * index(1)= 도수
     * @param doc 파싱 할 html 정보
     * @return 리스트 반환
     */
    public Map<String,String> isMap(Document doc) {
        Map<String,String> result = new HashMap<>();

        // 다양한 위치를 한 번에 커버 (items, spec_list, h_area, spec_set 등)
        Element container = doc.selectFirst(
                ".spec_list, .items, .h_area, .spec_set, .spec_set_wrap, #infoBottom, #productSpec, .prod_spec, .detail_info"
        );

        // 기본값: 못 찾으면 null
        String packaging = null;
        String alcohol   = null;

        if (container != null) {
            // 태그 구조가 제각각이라 안전하게 '텍스트'에서 정규식으로 추출
            String text = container.text();

            // 포장형태: "포장형태 : 페트" / "포장형태:페트" 등 변형 대응
            // 한글/영문/숫자/슬래시/하이픈/언더스코어 정도까지 허용
            Matcher pkgM = Pattern.compile("포장형태\\s*[:：]?\\s*([가-힣A-Za-z0-9/_\\-]+)")
                    .matcher(text);
            if (pkgM.find()) {
                packaging = pkgM.group(1).trim();
                if (packaging.isEmpty()) packaging = null;
            }

            // 도수: "도수: 6도" / "도수: 16%" 등 변형 대응 (숫자만 뽑기)
            Matcher alcM = Pattern.compile("도수\\s*[:：]?\\s*([0-9]{1,3})\\s*(?:도|%)")
                    .matcher(text);
            if (alcM.find()) {
                alcohol = alcM.group(1).trim(); // 숫자만
                if (alcohol.isEmpty()) alcohol = null;
            }
        }

        result.put("packaging", packaging);
        result.put("alcohol",   alcohol);
        return result;
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
