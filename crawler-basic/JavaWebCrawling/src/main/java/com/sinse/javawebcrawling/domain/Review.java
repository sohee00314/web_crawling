package com.sinse.javawebcrawling.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Review {
    //리뷰 작성자 아이디
    @JsonProperty("user_name")
    private String reviewer;
    //별점
    @JsonProperty("star")
    private int star;
    //쇼핑몰 이름
    @JsonProperty("shop_name")
    private String shopName;
    //작성일
    @JsonProperty("review_date")
    private String reviewDate;
    //제목
    @JsonProperty("title")
    private String title;
    //리뷰내용
    @JsonProperty("content")
    private String content;
    //리뷰 사진들
    @JsonProperty("photo_list")
    private List<String> photos = new ArrayList<>();
}
