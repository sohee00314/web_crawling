package com.sinse.javawebcrawling.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Product {
    //상품명
    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("spdNo")
    private String spdNo;
    @JsonProperty("sitmNo")
    private String sitmNo;

    //상품이미지
    @JsonProperty("image_url")
    private String imageUrl;
    //카테고리
    @JsonProperty("category")
    private String category;
    //가격
    @JsonProperty("price")
    private int price;
    //할인가
    @JsonProperty("discount")
    private int discount;
    //상세 링크
    @JsonProperty("detail_link")
    private String detailLink;
    //판매 장소
    @JsonProperty("market")
    private String market;

}
