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
    //상품이미지
    @JsonProperty("image_url")
    private String imageUrl;
    //주종
    @JsonProperty("category")
    private String category;
    //종류
    @JsonProperty("product_kind")
    private String productKind;
    //설명
    @JsonProperty("content")
    private String content;
    //도수
    @JsonProperty("alcohol")
    private int alcohol;
    //용량
    @JsonProperty("volume")
    private int volume;
    //포장상태
    @JsonProperty("packaging")
    private String packaging;
    //구성
    @JsonProperty("lineup")
    private String lineup;
    //상세 링크
    @JsonProperty("detail_link")
    private String detailLink;
    //가격
    @JsonProperty("prices")
    private List<Price> prices = new ArrayList<>();
    //상품리뷰들
    @JsonProperty("review_list")
    private List<Review> reviews = new ArrayList<>();

}
