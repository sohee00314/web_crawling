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
    //리뷰내용
    @JsonProperty("content")
    private String content;
    //리뷰 사진들
    @JsonProperty("photo_list")
    private List<String> photos = new ArrayList<>();
}
