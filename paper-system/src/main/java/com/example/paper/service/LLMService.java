package com.example.paper.service;

import com.example.paper.config.AppProperties;
import com.example.paper.util.CategoryConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public LLMService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public List<String> classify(String content) {
        if (content == null) {
            return Collections.emptyList();
        }

        String apiKey = appProperties.getLlm().getApiKey();
        String endpoint = appProperties.getLlm().getEndpoint();

        if (apiKey == null || apiKey.isBlank() || endpoint == null || endpoint.isBlank()) {
            // 没配置 LLM 时用关键词兜底，保证流程可跑通
            return keywordFallback(content);
        }

        String prompt = buildPrompt(content);
        try {
            String responseBody = callDashScope(prompt, apiKey, endpoint);
            return parseCategories(responseBody);
        } catch (Exception e) {
            // 调用失败时不要让上传流程完全不可用
            return keywordFallback(content);
        }
    }

    private String callDashScope(String prompt, String apiKey, String endpoint) throws IOException, InterruptedException {
        // DashScope / Qwen-plus 常见 body 形态（如果你的 endpoint 不匹配，可在 config 里改 endpoint）
        // 注意：不同厂商可能在 headers/请求体字段上有差异，这里做“尽量兼容”的实现
        var root = objectMapper.createObjectNode();
        root.put("model", appProperties.getLlm().getModel());

        var input = objectMapper.createObjectNode();
        input.put("prompt", prompt);
        root.set("input", input);

        var params = objectMapper.createObjectNode();
        params.put("result_format", appProperties.getLlm().getResponseFormat());
        root.set("parameters", params);

        String body = root.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("LLM call failed, status=" + response.statusCode());
        }
        return response.body();
    }

    private List<String> keywordFallback(String content) {
        String c = content.toLowerCase();
        Set<String> hits = new HashSet<>();

        boolean cf = c.contains("collaborative") || c.contains("matrix factorization") || c.contains("user-item") || c.contains("item-based")
                || c.contains("cf-based") || c.contains("recommendation") && c.contains("filter");
        boolean graph = c.contains("graph") || c.contains("gnn") || c.contains("node") || c.contains("edge") || c.contains("message passing");
        boolean context = c.contains("context") || c.contains("session") || c.contains("sequence") || c.contains("time-aware") || c.contains("situation");

        if (cf) hits.add("CF Based");
        if (graph) hits.add("Graph Based");
        if (context) hits.add("Context Based");

        if (hits.size() >= 2) {
            hits.add("Hybrid Based");
        }
        if (hits.isEmpty()) {
            hits.add("LLM Based");
        }
        // Hybrid Based 如果是由 hits.size()>=2 触发也属于正常多标签
        return new ArrayList<>(hits);
    }

    private String buildPrompt(String content) {
        return "你是推荐系统领域专家，请根据论文内容判断其属于以下哪些类别（可以多选）：\n" +
                "1. CF Based\n" +
                "2. Graph Based\n" +
                "3. Context Based\n" +
                "4. Hybrid Based\n" +
                "5. LLM Based\n\n" +
                "返回格式：\n" +
                "[\"CF Based\", \"Graph Based\"]\n\n" +
                "论文内容如下：\n" +
                content;
    }

    private List<String> parseCategories(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Collections.emptyList();
        }

        // 先从 JSON 结构里找可能的 text 字段
        String text = extractTextFromJson(responseBody);
        if (text == null) {
            text = responseBody;
        }

        // 抽取形如 ["CF Based","Graph Based"] 的数组片段
        Pattern p = Pattern.compile("\\[[^\\]]*\\]");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String arr = m.group();
            try {
                List<String> parsed = objectMapper.readValue(arr, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                return normalize(parsed);
            } catch (Exception ignored) {
                // fallthrough
            }
        }

        // 兜底：直接匹配固定枚举名出现情况
        Set<String> results = new HashSet<>();
        for (String c : CategoryConstants.DEFAULT_CATEGORIES) {
            if (text.contains(c)) {
                results.add(c);
            }
        }
        if (results.isEmpty()) {
            results.add("LLM Based");
        }
        return new ArrayList<>(results);
    }

    private String extractTextFromJson(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // 常见：output.text 或 output.choices[0].message.content
            if (root.has("output") && root.get("output").has("text")) {
                return root.get("output").get("text").asText();
            }
            if (root.has("output") && root.get("output").has("choices")) {
                JsonNode choices = root.get("output").get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode first = choices.get(0);
                    if (first.has("message") && first.get("message").has("content")) {
                        return first.get("message").get("content").asText();
                    }
                }
            }
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode first = root.get("choices").get(0);
                if (first.has("message") && first.get("message").has("content")) {
                    return first.get("message").get("content").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<String> normalize(List<String> parsed) {
        if (parsed == null) {
            return Collections.emptyList();
        }
        Set<String> allowed = new HashSet<>(CategoryConstants.DEFAULT_CATEGORIES);
        List<String> out = new ArrayList<>();
        for (String s : parsed) {
            if (s != null) {
                String trimmed = s.trim();
                if (allowed.contains(trimmed)) {
                    out.add(trimmed);
                }
            }
        }
        if (out.isEmpty()) {
            out.add("LLM Based");
        }
        return out;
    }
}

