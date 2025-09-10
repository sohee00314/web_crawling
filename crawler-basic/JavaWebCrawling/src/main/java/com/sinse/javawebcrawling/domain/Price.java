package com.sinse.javawebcrawling.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Price {
    //상품구매 링크
    @JsonProperty("shop_link")
    private String shopLink;
    //쇼핑몰 이름
    @JsonProperty("shop_name")
    private String shopName;
    //상품 가격
    @JsonProperty("price")
    private int price;
    //배송비
    @JsonProperty("delivery_fee")
    private int deliveryFee;
}
