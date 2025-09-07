package com.sinse.javawebcrawling.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sinse.javawebcrawling.domain.Product;
import com.sinse.javawebcrawling.util.InitialDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WebCrawlingService {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT = 10000;

    public List<Product> lotteCrawler(String lotteUrl) throws IOException {
    List<Product> products = new ArrayList<>();
        String url = lotteUrl;
        Document doc;
        if (lotteUrl != null && (lotteUrl.startsWith("http://") || lotteUrl.startsWith("https://"))){
            log.info("Url로 판단하여 파싱 시도");
            doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();

        }else {
            log.info("HTML 문자열로 판단하여 파싱 시도");
            String html = lotteUrl != null ? lotteUrl : "";
            doc = Jsoup.parse(html, "");
        }
        log.debug(doc.body().html());

        ArrayNode initial = InitialDataUtil.findInitialData(doc);
        if (initial == null) {
            log.debug("initialData not found (렌더링 후 주입되거나, 키 이름이 다를 수 있음)");
        } else {
            log.debug("initialData size= {}", initial.size());
            for (int i = 0; i < initial.size(); i++) {
                Product product = new Product();
                JsonNode o = initial.get(i);
                String name  = o.path("productName").asText(o.path("spdNm").asText(null));
                String detailLink  = o.path("productLink").asText(null);
                String imgLink  = o.path("productImage").asText(null);
                String market = o.path("storeName").asText(null);


                Integer price = null;
                String spdNo = null;
                String sitmNo = null;
                String category =
                        o.path("categoryName").asText(null); // 1순위: 최상위에 있으면 사용
                if (category == null || category.isBlank()) {
                    category = o.path("brazeData").path("categoryName").asText(null); // 2순위: brazeData 안
                    spdNo =  o.path("brazeData").path("spdNo").asText(null);
                    sitmNo= o.path("brazeData").path("sitmNo").asText(null);
                }

                JsonNode pi = o.get("priceInfo");
                if (pi != null && pi.isArray()) {
                    for (JsonNode e : pi) {
                        if ("final".equals(e.path("type").asText())) {
                            price = e.path("num").asInt();
                            break;
                        }
                    }
                }
                if (price == null) {
                    if (o.has("discountPrice")) price = o.get("discountPrice").asInt();
                    else if (o.has("price"))    price = o.get("price").asInt();
                }

                //상품정보 정의
                product.setProductName(name);
                product.setPrice(price);
                product.setCategory(category);
                product.setDetailLink(detailLink);
                product.setImageUrl(imgLink);
                product.setMarket(market);
                product.setSitmNo(sitmNo);
                product.setSpdNo(spdNo);
                products.add(product);

            }
        }
        log.info("=== 크롤링 완료: {}개 상품 파싱 ===", products.size());
        return products;
    }

}
