package com.sinse.javawebcrawling.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Product {
    //상품명
    @JsonProperty("product_name")
    private String productName;
    //상품코드
    @JsonProperty("product_code")
    private int code;
    //브랜드
    @JsonProperty("brand")
    private String brand;
    //상품이미지
    @JsonProperty("image_url")
    private String imageUrl;
    //카테고리
    @JsonProperty("category")
    private String category;
    //상세 링크
    @JsonProperty("detail_link")
    private String detailLink;
    //상세정보
    @JsonProperty("content")
    private String content;
    //가격
    @JsonProperty("prices")
    private List<Price> prices = new ArrayList<>();

    //상품리뷰들
    @JsonProperty("review_list")
    private List<Review> reviews = new ArrayList<>();

}
