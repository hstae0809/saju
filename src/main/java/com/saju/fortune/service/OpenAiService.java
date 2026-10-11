package com.saju.fortune.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saju.fortune.dto.FortuneResponseDto;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FortuneResponseDto getFortune(SajuRequest req, SajuPalja palja, String grade, int magicNumber) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_DEFAULT_API_KEY")) {
            return createFallbackResponse(req, palja, grade, magicNumber);
        }

        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String charName = getCharacterName(req.getCharacter());
            String charTone = getCharacterTone(req.getCharacter());

            String systemPrompt = String.format(
                "너는 '행운길드'의 동반자 고양이 캐릭터 '%s'야. %s\n" +
                "사용자의 사주팔자(8글자)와 오늘 운세 등급(%s)을 바탕으로 친근하고 다정한 오늘의 운세를 JSON으로 작성해줘.\n" +
                "반드시 아래 JSON 형식만 출력해야 해:\n" +
                "{\n" +
                "  \"title\": \"한 줄 제목\",\n" +
                "  \"description\": \"한 줄 총평 요약\",\n" +
                "  \"details\": [\"상세 문장 1\", \"상세 문장 2\"],\n" +
                "  \"action\": \"오늘 추천하는 작은 행동 하나\",\n" +
                "  \"luckyItem\": \"행운 아이템\",\n" +
                "  \"luckyColor\": \"행운 컬러\",\n" +
                "  \"quote\": \"%s 어조의 말풍선 한마디\"\n" +
                "}",
                charName, charTone, grade, charName
            );

            String userPrompt = String.format(
                "사용자 정보:\n- 캐릭터: %s\n- 사주 8글자: %s\n- 운세 등급: %s (점수: %d점)\n- 생년월일시: %d년 %d월 %d일 %d시\n\n" +
                "위 정보를 토대로 오늘의 운세 JSON을 생성해줘.",
                charName, palja.getPaljaString(), grade, magicNumber,
                req.getResolvedYear(), req.getResolvedMonth(), req.getResolvedDay(), req.getResolvedHour()
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

                    return new FortuneResponseDto(
                        grade,
                        (String) parsed.getOrDefault("title", "뜻밖의 반가움이 찾아오는 날"),
                        (String) parsed.getOrDefault("description", "작은 즐거움이 기다리고 있어요."),
                        (List<String>) parsed.getOrDefault("details", List.of("마음의 여유를 가져보세요.", "새로운 하루가 펼쳐집니다.")),
                        (String) parsed.getOrDefault("action", "좋아하는 음악을 들으며 산책해 보세요."),
                        (String) parsed.getOrDefault("luckyItem", "열쇠고리"),
                        (String) parsed.getOrDefault("luckyColor", "하늘색"),
                        (String) parsed.getOrDefault("quote", "오늘도 내가 함께할게!"),
                        palja.getPaljaString(),
                        magicNumber
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("OpenAI API 호출 에러, 로컬 Fallback으로 대체: " + e.getMessage());
        }

        return createFallbackResponse(req, palja, grade, magicNumber);
    }

    public FortuneResponseDto createFallbackResponse(SajuRequest req, SajuPalja palja, String grade, int magicNumber) {
        String character = req.getCharacter();
        String quote;
        String title;
        String desc;
        List<String> details;
        String action;
        String item;
        String color;

        if ("good".equals(grade)) {
            title = "뜻밖의 반가움이 찾아오는 날";
            desc = "평소 같으면 지나쳤을 곳에서 작은 즐거움을 발견할지도 몰라요.";
            details = List.of(
                "궁금했던 일이나 망설였던 제안이 있다면 가볍게 한 걸음 내디뎌 보세요.",
                "거창한 도전보다 작은 시도가 즐거운 하루를 만들어줄 거예요."
            );
            action = "궁금해서 저장만 해뒀던 것 하나를 해보세요.";
            item = "열쇠고리";
            color = "하늘색";
            quote = switch (character) {
                case "mage" -> "작은 변화도 놓치지 않는 게 마법의 시작이야!";
                case "healer" -> "오늘 너의 미소가 주변을 따뜻하게 물들일 거야.";
                default -> "작은 발견도 모이면 멋진 모험이 된다고!";
            };
        } else if ("normal".equals(grade)) {
            title = "평범함 속에서 발견하는 작은 행복";
            desc = "오늘은 소소한 일상이 마음을 편안하게 해줄 거예요.";
            details = List.of(
                "서두르지 않고 자신의 속도를 지켜보세요.",
                "익숙한 풍경에서도 새로운 기분을 발견할 수 있어요."
            );
            action = "좋아하는 음악을 들으며 산책해 보세요.";
            item = "머그컵";
            color = "연두색";
            quote = switch (character) {
                case "mage" -> "서두르지 않아도 모든 건 때가 있는 법이야.";
                case "healer" -> "오늘은 천천히 가도 괜찮아, 네 곁에 있을게.";
                default -> "한 걸음씩 가다 보면 어느새 도착해 있을걸!";
            };
        } else {
            title = "조금은 신중하게 걸어야 하는 날";
            desc = "생각했던 것과 다른 일이 생겨도 조급해하지 마세요.";
            details = List.of(
                "오늘은 중요한 결정을 서두르지 않는 것이 좋겠어요.",
                "충분한 휴식으로 내일을 준비해 보세요."
            );
            action = "따뜻한 음료를 마시며 잠깐 쉬어보세요.";
            item = "손수건";
            color = "보라색";
            quote = switch (character) {
                case "mage" -> "때로는 가만히 관찰하는 것이 최고의 마법이지.";
                case "healer" -> "마음에 쉼표가 필요한 날이야, 무리하지 마.";
                default -> "모험도 좋지만 쉬어 가는 날도 필요하지!";
            };
        }

        return new FortuneResponseDto(
            grade, title, desc, details, action, item, color, quote, palja.getPaljaString(), magicNumber
        );
    }

    private String getCharacterName(String character) {
        if (character == null) return "용사냥";
        return switch (character) {
            case "mage" -> "마법사냥";
            case "healer" -> "치유냥";
            default -> "용사냥";
        };
    }

    private String getCharacterTone(String character) {
        if (character == null) return "씩씩하고 솔직하며 모험심 넘치고 활기찬 어조";
        return switch (character) {
            case "mage" -> "호기심 많고 섬세하며 신비로운 관찰자 어조 (반말, 다정함)";
            case "healer" -> "차분하고 따뜻하며 공감과 위로를 건네는 어조 (반말, 다정함)";
            default -> "씩씩하고 솔직하며 모험심 넘치고 활기찬 어조 (반말, 용기 북돋움)";
        };
    }
}
