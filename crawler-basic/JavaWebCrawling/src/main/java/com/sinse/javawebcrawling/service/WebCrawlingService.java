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

    public String lotteCrawler() throws IOException {
        List<Product> products = new ArrayList<>();
        String lotteUrl = "https://www.lotteon.com/csearch/render/category?render=nqapi&platform=pc&collection_id=9&login=Y&u9=navigate&u8=FC01220406&mallId=1";
        Document doc = Jsoup.connect(lotteUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .get();
        // log.debug("{}", doc.body().html());

        ArrayNode initial = InitialDataUtil.findInitialData(doc);
        if (initial == null) {
            log.debug("initialData not found (렌더링 후 주입되거나, 키 이름이 다를 수 있음)");
        } else {
            log.debug("initialData size= {}", initial.size());
            for (int i = 0; i < initial.size(); i++) {
                JsonNode o = initial.get(i);
                String name  = o.path("productName").asText(o.path("spdNm").asText(null));
                String detailLink  = o.path("productLink").asText(null);
                String imgLink  = o.path("productImage").asText(null);
                String market = o.path("storeName").asText(null);
                String brand = o.path("brandName").asText(o.path("brand").asText(null));
                Integer price = null;
                String category =
                        o.path("categoryName").asText(null); // 1순위: 최상위에 있으면 사용
                if (category == null || category.isBlank()) {
                    category = o.path("brazeData").path("categoryName").asText(null); // 2순위: brazeData 안
                }
                if (category == null || category.isBlank()) {
                    // 3순위(백업): 코드만 있을 때는 data.category(=categoryNo)라도 확보
                    category = o.path("data").path("category").asText(null); // 예: "BC67060600"
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
                log.debug("name={}, brand={}, price={}, detailLink={}, category={}, imgLink={}, market= {}", name, brand, price, detailLink,category,imgLink,market);
                // 필요한 경우 products 리스트에 담을 수 있음
                // products.add(new Product(name, link, brand, price));
            }
        }

        return null;
    }

}
