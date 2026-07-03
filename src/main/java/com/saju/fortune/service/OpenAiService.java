package com.saju.fortune.service;

import com.saju.fortune.dto.SajuPalja;
import com.saju.fortune.dto.SajuRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getFortuneDescription(SajuRequest req, SajuPalja palja) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_DEFAULT_API_KEY")) {
            throw new IllegalStateException("OpenAI API Key가 설정되지 않았습니다.");
        }

        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String genderKorean = "M".equalsIgnoreCase(req.getGender()) ? "남성" : "여성";
        String systemPrompt = "너는 전문 명리학자이자 사주풀이 전문가야. 사용자의 사주팔자(8글자)와 오행 분석 결과를 바탕으로 오늘의 운세를 친절하고 전문적으로 한국어로 풀이해줘.";
        String userPrompt = String.format(
            "사용자 정보:\n- 생년월일시: %d년 %d월 %d일 %d시\n- 성별: %s\n- 사주팔자 8글자: %s\n\n" +
            "위의 사주 정보를 바탕으로 다음 4가지 항목을 나누어 정성스럽게 오늘의 운세를 작문해줘:\n" +
            "1. 오늘의 총평\n2. 재물운\n3. 연애운\n4. 건강운\n" +
            "답변은 친근하면서도 깊이 있는 어조의 한국어로 해주고, 마크다운 포맷을 적절히 활용해줘.",
            req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(), req.getBirthTime(),
            genderKorean, palja.getPaljaString()
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            throw new RuntimeException("API 응답이 비어있거나 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }
}
