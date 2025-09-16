package com.sinse.javawebcrawling.util;

import org.springframework.beans.factory.annotation.Value;

public class FinalUrlResolver {
    //본인에 맞는 사용자 에이전트 설정
    @Value("${user-agent}")
    private String UA;

    public String resolveUrl(String url) {
        return url;
    }
}
