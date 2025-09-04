package com.sinse.javawebcrawling.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class InitialDataUtil {
    // JS-풍 문법(작은따옴표, 트레일링 콤마, 주석, 미인용 필드명)을 허용하는 팩토리
    private static final ObjectMapper OM = new ObjectMapper(
            JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                    .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .build()
    );

    /** 문서에서 initialData 배열(JSON) 찾기 */
    public static ArrayNode findInitialData(Document doc) {
        // 1) 진짜 JSON 스크립트 먼저 시도
        for (Element s : doc.select("script[type=application/json]")) {
            ArrayNode arr = fromJsonScript(s.data(), "initialData");
            if (arr != null) return arr;
        }
        // 2) 일반 자바스크립트에서 추출 (type=application/javascript 또는 type 없음)
        for (Element s : doc.select("script[type=application/javascript], script:not([type])")) {
            String code = s.data();
            if (!code.contains("initialData")) continue;

            // JSON.parse('...') 패턴 우선 처리
            ArrayNode arr = fromJsonParseCall(code, "initialData");
            if (arr != null) return arr;

            // key: [ ... ] 패턴 추출 (괄호 밸런싱)
            arr = extractArrayAfterKey(code, "initialData");
            if (arr != null) return arr;
        }
        return null;
    }

    private static ArrayNode fromJsonScript(String json, String key) {
        try {
            JsonNode root = OM.readTree(json);
            JsonNode node = root.path(key);
            return node.isArray() ? (ArrayNode) node : null;
        } catch (Exception e) {
            return null;
        }
    }

    // initialData = JSON.parse('...') 같은 경우: 따옴표 안 JSON 문자열을 언이스케이프 후 파싱
    private static ArrayNode fromJsonParseCall(String code, String key) {
        // 매우 단순한 패턴 매칭 (필요하면 정규식 강화 가능)
        int k = code.indexOf(key);
        if (k < 0) return null;
        int parse = code.indexOf("JSON.parse", k);
        if (parse < 0) return null;
        int open = code.indexOf('(', parse);
        int close = code.indexOf(')', open + 1);
        if (open < 0 || close < 0) return null;

        String inside = code.substring(open + 1, close).trim();
        // 따옴표 제거
        if ((inside.startsWith("'") && inside.endsWith("'")) ||
                (inside.startsWith("\"") && inside.endsWith("\""))) {
            inside = inside.substring(1, inside.length() - 1);
        }
        // JS 문자열 언이스케이프(필요 최소한)
        inside = inside.replace("\\\"", "\"").replace("\\\\", "\\");
        try {
            JsonNode node = OM.readTree(inside);
            return node.isArray() ? (ArrayNode) node
                    : node.has("initialData") && node.get("initialData").isArray()
                    ? (ArrayNode) node.get("initialData") : null;
        } catch (Exception e) {
            return null;
        }
    }

    // initialData: [ ... ] 형태를 괄호 밸런싱으로 정확히 잘라 파싱
    private static ArrayNode extractArrayAfterKey(String code, String key) {
        int k = code.indexOf(key);
        if (k < 0) return null;

        // key 이후 첫 '[' 찾기 (콜론/공백/= 등은 건너뜀)
        int start = code.indexOf('[', k);
        if (start < 0) return null;

        int depth = 0;
        boolean inStr = false;
        char quote = 0;

        for (int i = start; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inStr) {
                if (c == quote && code.charAt(i - 1) != '\\') inStr = false;
            } else {
                if (c == '"' || c == '\'') { inStr = true; quote = c; }
                else if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        String jsonArr = code.substring(start, i + 1);
                        // JS 특유 토큰 정리 (최소한)
                        jsonArr = jsonArr.replaceAll("\\bundefined\\b", "null");
                        try {
                            JsonNode node = OM.readTree(jsonArr);
                            return node.isArray() ? (ArrayNode) node : null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
}
