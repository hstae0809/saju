package com.saju.fortune.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saju.fortune.domain.Element;
import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuPalja;
import com.saju.fortune.dto.SajuRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, FortuneResponseDto> dailyCache = new ConcurrentHashMap<>();

    public OpenAiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    public FortuneResponseDto getFortune(SajuRequest req, SajuPalja palja, String grade, int magicNumber) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("YOUR_DEFAULT_API_KEY")) {
            return createFallbackResponse(req, palja, grade, magicNumber);
        }

        String cacheKey = LocalDate.now() + "|" + req.getCharacter() + "|" + palja.getPaljaString() + "|"
            + req.getResolvedYear() + "-" + req.getResolvedMonth() + "-" + req.getResolvedDay() + "-" + req.getResolvedHour();

        FortuneResponseDto cached = dailyCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String charName = getCharacterName(req.getCharacter());
            String charTone = getCharacterTone(req.getCharacter());
            Element dayElement = palja.dayCheongan.getElement();
            Element neededElement = findNeededElement(palja);

            String systemPrompt = String.format(
                "너는 '행운길드'의 동반자 고양이 캐릭터 '%s'야. %s\n" +
                "사용자의 사주팔자(8글자), 타고난 일간 기운(%s), 보완이 필요한 오행(%s), 오늘 운세 등급(%s)을 바탕으로 친근하고 다정한 오늘의 운세를 JSON으로 작성해줘.\n" +
                "반드시 아래 JSON 형식만 출력해야 해:\n" +
                "{\n" +
                "  \"title\": \"한 줄 제목\",\n" +
                "  \"description\": \"한 줄 총평 요약\",\n" +
                "  \"details\": [\"타고난 기운과 오늘의 흐름을 설명하는 문장 1\", \"오늘의 조언 문장 2\"],\n" +
                "  \"action\": \"오늘 추천하는 작은 행동 하나\",\n" +
                "  \"luckyItem\": \"행운 아이템 (단어)\",\n" +
                "  \"luckyColor\": \"행운 컬러 (단어)\",\n" +
                "  \"quote\": \"%s 어조의 말풍선 한마디\"\n" +
                "}",
                charName, charTone, dayElement.getKorean(), neededElement.getKorean(), grade, charName
            );

            String userPrompt = String.format(
                "사용자 정보:\n- 캐릭터: %s\n- 사주 8글자: %s\n- 일간(타고난 중심 기운): %s(%s)\n- 보완 오행: %s\n- 운세 등급: %s (점수: %d점)\n- 생년월일시: %d년 %d월 %d일 %d시\n\n" +
                "위 정보를 토대로 오늘의 운세 JSON을 생성해줘.",
                charName, palja.getPaljaString(), palja.dayCheongan.getName(), dayElement.getKorean(),
                neededElement.getKorean(), grade, magicNumber,
                req.getResolvedYear(), req.getResolvedMonth(), req.getResolvedDay(), req.getResolvedHour()
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.75);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

                    FortuneResponseDto fallback = createFallbackResponse(req, palja, grade, magicNumber);

                    FortuneResponseDto result = new FortuneResponseDto(
                        grade,
                        (String) parsed.getOrDefault("title", fallback.getTitle()),
                        (String) parsed.getOrDefault("description", fallback.getDescription()),
                        (List<String>) parsed.getOrDefault("details", fallback.getDetails()),
                        (String) parsed.getOrDefault("action", fallback.getAction()),
                        (String) parsed.getOrDefault("luckyItem", fallback.getLuckyItem()),
                        (String) parsed.getOrDefault("luckyColor", fallback.getLuckyColor()),
                        (String) parsed.getOrDefault("quote", fallback.getQuote()),
                        palja.getPaljaString(),
                        magicNumber
                    );
                    if (dailyCache.size() > 2000) {
                        dailyCache.clear();
                    }
                    dailyCache.put(cacheKey, result);
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("OpenAI API 호출 불가 (자체 사주 오행 엔진으로 자동 전환)");
        }

        return createFallbackResponse(req, palja, grade, magicNumber);
    }

    public FortuneResponseDto createFallbackResponse(SajuRequest req, SajuPalja palja, String grade, int magicNumber) {
        LocalDate today = LocalDate.now();
        String character = req.getCharacter() == null ? "warrior" : req.getCharacter();
        Element dayElement = palja.dayCheongan.getElement();
        Element neededElement = findNeededElement(palja);

        int seed1 = Math.floorMod(Objects.hash(
            palja.getPaljaString(), character, req.getResolvedYear(), req.getResolvedMonth(), req.getResolvedDay(), today.getDayOfYear()
        ), 10000);
        int seed2 = Math.floorMod(Objects.hash(
            palja.dayCheongan.ordinal(), palja.dayJiji.ordinal(), palja.hourCheongan.ordinal(), palja.hourJiji.ordinal(), magicNumber
        ), 10000);
        int seed3 = Math.floorMod(Objects.hash(
            palja.yearCheongan.ordinal(), palja.monthJiji.ordinal(), character, req.getResolvedHour(), today.getDayOfMonth()
        ), 10000);

        String[][] titleAndDescPool = getTitleAndDescPool(grade);
        String[] chosenTitleDesc = titleAndDescPool[seed1 % titleAndDescPool.length];
        String title = chosenTitleDesc[0];
        String desc = chosenTitleDesc[1];

        String detail1 = getDayElementDetail(dayElement, grade, seed2);
        String detail2 = getNeededElementAdvice(neededElement, grade, seed3);
        List<String> details = List.of(detail1, detail2);

        List<String> actions = getElementActions(neededElement, grade);
        String action = actions.get((seed1 + seed2) % actions.size());

        List<String> items = getElementLuckyItems(neededElement);
        String item = items.get(seed2 % items.size());

        List<String> colors = getElementLuckyColors(neededElement);
        String color = colors.get(seed3 % colors.size());

        List<String> quotes = getCharacterQuotes(character, grade);
        String quote = quotes.get((seed1 + seed3) % quotes.size());

        return new FortuneResponseDto(
            grade, title, desc, details, action, item, color, quote, palja.getPaljaString(), magicNumber
        );
    }

    private Element findNeededElement(SajuPalja palja) {
        EnumMap<Element, Integer> counts = new EnumMap<>(Element.class);
        for (Element e : Element.values()) {
            counts.put(e, 0);
        }
        counts.put(palja.yearCheongan.getElement(), counts.get(palja.yearCheongan.getElement()) + 1);
        counts.put(palja.yearJiji.getElement(), counts.get(palja.yearJiji.getElement()) + 1);
        counts.put(palja.monthCheongan.getElement(), counts.get(palja.monthCheongan.getElement()) + 1);
        counts.put(palja.monthJiji.getElement(), counts.get(palja.monthJiji.getElement()) + 1);
        counts.put(palja.dayCheongan.getElement(), counts.get(palja.dayCheongan.getElement()) + 1);
        counts.put(palja.dayJiji.getElement(), counts.get(palja.dayJiji.getElement()) + 1);
        counts.put(palja.hourCheongan.getElement(), counts.get(palja.hourCheongan.getElement()) + 1);
        counts.put(palja.hourJiji.getElement(), counts.get(palja.hourJiji.getElement()) + 1);

        Element[] order = Element.values();
        int startOffset = palja.dayCheongan.ordinal() % order.length;
        Element minElement = order[startOffset];
        int minCount = counts.get(minElement);

        for (int i = 1; i < order.length; i++) {
            Element candidate = order[(startOffset + i) % order.length];
            if (counts.get(candidate) < minCount) {
                minCount = counts.get(candidate);
                minElement = candidate;
            }
        }
        return minElement;
    }

    private String[][] getTitleAndDescPool(String grade) {
        if ("good".equals(grade)) {
            return new String[][] {
                {"뜻밖의 반가움이 찾아오는 날", "평소 같으면 지나쳤을 곳에서 기분 좋은 행운을 발견할지도 몰라요."},
                {"순풍에 돛을 단 듯 가벼운 하루", "마음먹었던 일들이 막힘없이 자연스럽게 풀려가는 흐름이에요."},
                {"반짝이는 영감이 발끝을 스치는 날", "스쳐 지나가던 작은 아이디어가 오늘 하루를 특별하게 빛내줄 거예요."},
                {"따뜻한 인연과 웃음이 머무는 날", "주변 사람들과 주고받는 다정한 말 한마디에 큰 에너지가 담겨 있어요."},
                {"기다렸던 타이밍이 눈앞에 열리는 날", "망설이던 일이 있다면 오늘 가볍게 첫걸음을 내디뎌도 좋아요."},
                {"발걸음마다 작은 행운이 따라붙는 날", "계획하지 않았던 우연한 선택이 오히려 더 멋진 결과를 가져다줄 거예요."}
            };
        } else if ("normal".equals(grade)) {
            return new String[][] {
                {"평범함 속에서 발견하는 작은 행복", "오늘은 소소한 일상이 마음을 잔잔하고 편안하게 채워줄 거예요."},
                {"나만의 리듬으로 천천히 걷는 하루", "남들과 속도를 맞추기보다 내 호흡대로 움직일 때 가장 빛나는 날이에요."},
                {"익숙한 풍경이 새롭게 다가오는 날", "매일 지나던 길이나 일상 속에서 소소한 여유와 기쁨을 찾기 좋아요."},
                {"차분히 정돈하며 에너지를 모으는 날", "밀려 있던 작은 일들을 하나씩 정리하면 마음까지 한결 가벼워질 거예요."},
                {"잔잔한 호수처럼 평온함이 머무는 날", "크게 무리하지 않아도 계획한 만큼 안정적으로 하루가 흘러갈 거예요."},
                {"소소한 취향과 휴식이 빛을 발하는 날", "내가 좋아하는 작은 습관 하나가 오늘 하루의 든든한 버팀목이 되어줄 거예요."}
            };
        } else {
            return new String[][] {
                {"조금은 신중하게 걸어야 하는 날", "생각했던 것과 다른 흐름이 생겨도 조급해하지 않으면 충분히 괜찮아요."},
                {"마음에 잠시 쉼표를 찍어주어야 할 날", "모든 것을 오늘 다 해내려 하기보다 한 템포 쉬어 가는 지혜가 필요해요."},
                {"비 온 뒤 땅이 굳어지듯 차분해지는 날", "사소한 변수가 생기더라도 당황하지 않고 천천히 살피면 금방 지나갈 거예요."},
                {"나를 먼저 다독이고 아껴주어야 하는 날", "주변의 시선이나 조급한 마음은 잠시 내려놓고 내 컨디션을 먼저 챙겨보세요."},
                {"서두름 대신 여유가 해답이 되는 날", "중요한 결정이나 약속은 한 번 더 확인하고 여유 있게 움직이는 것이 좋아요."},
                {"내일의 도약을 위해 숨을 고르는 날", "오늘은 에너지를 아끼며 일찍 휴식을 취하는 것만으로도 충분히 멋진 하루예요."}
            };
        }
    }

    private String getDayElementDetail(Element dayElement, String grade, int seed) {
        return switch (dayElement) {
            case WOOD -> switch (grade) {
                case "good" -> seed % 2 == 0
                    ? "타고난 나무(木)의 생장 기운이 오늘의 운세와 만나 새로운 시작과 도전에 활력을 불어넣어 주고 있어요."
                    : "곧게 뻗어 나가는 나무(木)의 기운이 활발해져 평소 궁금했던 일을 시작하기에 더없이 좋은 흐름이에요.";
                case "normal" -> seed % 2 == 0
                    ? "나무(木)의 차분한 성장 기운이 머물러 있어 서두르지 않고 꾸준히 할 일을 이어가기에 좋은 날이에요."
                    : "타고난 나무(木)의 유연한 기운 덕분에 예상치 못한 상황도 부드럽게 넘길 수 있는 하루예요.";
                default -> seed % 2 == 0
                    ? "나무(木)의 기운이 바람에 흔들릴 수 있는 날이니 가지를 넓히기보다 뿌리를 단단히 다지는 데 집중해 보세요."
                    : "의욕이 앞서면 쉽게 피로해질 수 있으니 오늘만큼은 무리한 계획보다 현재의 리듬을 지키는 것이 좋아요.";
            };
            case FIRE -> switch (grade) {
                case "good" -> seed % 2 == 0
                    ? "타고난 불(火)의 밝고 따뜻한 에너지가 환하게 피어나 주변 사람들에게도 좋은 인상을 남기는 날이에요."
                    : "열정적인 불(火)의 기운이 오늘의 흐름과 어우러져 표현력과 추진력이 한층 돋보이는 하루가 될 거예요.";
                case "normal" -> seed % 2 == 0
                    ? "불(火)의 온화한 온기가 은은하게 유지되어 마음을 편안하게 먹으면 일상이 순조롭게 풀려갈 거예요."
                    : "타고난 불(火)의 밝은 기운을 차분한 일상에 녹여내면 소소하지만 확실한 성취감을 느낄 수 있어요.";
                default -> seed % 2 == 0
                    ? "불(火)의 기운이 다소 급해질 수 있는 날이니 감정적인 반응이나 성급한 판단은 잠시 미뤄두는 것이 좋아요."
                    : "마음의 열기가 과열되지 않도록 깊게 호흡하며 차분하게 주변을 돌아보는 여유가 필요한 하루예요.";
            };
            case EARTH -> switch (grade) {
                case "good" -> seed % 2 == 0
                    ? "타고난 흙(土)의 든든하고 포용력 있는 기운이 빛을 발해 신뢰와 행운이 자연스럽게 모여드는 날이에요."
                    : "단단한 대지 같은 흙(土)의 에너지가 안정감을 주어 그동안 쌓아온 노력이 기분 좋은 결실로 이어질 거예요.";
                case "normal" -> seed % 2 == 0
                    ? "흙(土)의 묵직하고 평온한 기운이 중심을 잡아주어 흔들림 없이 나만의 페이스를 유지할 수 있는 날이에요."
                    : "익숙한 공간과 사람들 속에서 타고난 흙(土)의 안정감이 더욱 빛나며 마음이 한결 편안해질 거예요.";
                default -> seed % 2 == 0
                    ? "흙(土)의 기운이 다소 무거워질 수 있으니 너무 많은 책임을 혼자 떠안기보다 짐을 조금 덜어내 보세요."
                    : "고집스럽게 한 가지 방법만 고수하기보다 유연하게 생각을 열어두면 답답했던 흐름이 한결 가벼워질 거예요.";
            };
            case METAL -> switch (grade) {
                case "good" -> seed % 2 == 0
                    ? "타고난 쇠(金)의 명쾌하고 깔끔한 결단력이 오늘의 운과 맞아떨어져 복잡한 일도 시원하게 해결될 거예요."
                    : "보석처럼 단단하게 빛나는 쇠(金)의 기운이 강해져 집중력과 판단력이 최고조에 이르는 기분 좋은 날이에요.";
                case "normal" -> seed % 2 == 0
                    ? "쇠(金)의 정돈된 기운이 흐르고 있어 어수선했던 주변이나 생각을 깔끔하게 정리하기에 알맞은 하루예요."
                    : "타고난 쇠(金)의 분별력 덕분에 불필요한 일에 에너지를 뺏기지 않고 알차게 하루를 보낼 수 있어요.";
                default -> seed % 2 == 0
                    ? "쇠(金)의 날카로운 기운이 강해질 수 있으니 나 자신이나 타인에게 너무 엄격한 잣대를 대기보다 너그러워져 보세요."
                    : "완벽하게 해내려는 부담감이 피로를 부를 수 있으니 오늘은 조금 빈틈이 있어도 괜찮다고 스스로를 다독여 주세요.";
            };
            case WATER -> switch (grade) {
                case "good" -> seed % 2 == 0
                    ? "타고난 물(水)의 지혜롭고 유연한 기운이 흐르며 막혔던 곳도 자연스럽게 돌아 나가는 행운의 날이에요."
                    : "맑은 물(水)처럼 총명한 직관력이 반짝여서 좋은 아이디어나 반가운 소식을 누구보다 빠르게 포착할 수 있어요.";
                case "normal" -> seed % 2 == 0
                    ? "물(水)의 잔잔하고 깊은 기운이 마음을 감싸주어 조용히 사색하거나 나만의 시간에 몰두하기 좋은 날이에요."
                    : "흐르는 물(水)처럼 상황에 자연스럽게 스며들면 큰 힘을 들이지 않고도 평온한 하루를 보낼 수 있어요.";
                default -> seed % 2 == 0
                    ? "물(水)의 기운이 깊어져 생각이 많아질 수 있는 날이니 일어나지 않은 일로 미리 걱정하지 않아도 괜찮아요."
                    : "감정의 물결이 일렁일 때는 억지로 답을 찾으려 하기보다 따뜻한 공간에서 몸과 마음을 먼저 녹여보세요.";
            };
        };
    }

    private String getNeededElementAdvice(Element neededElement, String grade, int seed) {
        return switch (neededElement) {
            case WOOD -> seed % 2 == 0
                ? "오늘 부족한 나무(木)의 생기를 채워주면 더 좋은 흐름이 찾아오니 푸른 식물이나 신선한 공기를 가까이해 보세요."
                : "거창한 계획보다 작은 호기심을 따라가며 나무(木)의 활기찬 시작 에너지를 일상에 살짝 더해 보세요.";
            case FIRE -> seed % 2 == 0
                ? "오늘 보완하면 좋은 불(火)의 온기를 더하기 위해 밝은 표정과 따뜻한 말 한마디로 주변의 분위기를 밝혀보세요."
                : "마음속 작은 설렘을 외면하지 말고 불(火)의 활기찬 기운처럼 기분 전환이 되는 즐거운 일을 하나 찾아보세요.";
            case EARTH -> seed % 2 == 0
                ? "오늘 필요한 흙(土)의 안정감을 채워줄 수 있도록 여유로운 식사와 편안한 휴식 시간을 꼭 챙겨보세요."
                : "익숙하고 편안한 공간에서 흙(土)의 든든한 기운을 느끼며 마음의 중심을 차분히 가다듬으면 행운이 머물 거예요.";
            case METAL -> seed % 2 == 0
                ? "오늘 보완이 필요한 쇠(金)의 정돈된 기운을 불러오려면 가방이나 책상 위를 가볍게 정리하며 산뜻하게 출발해 보세요."
                : "우선순위를 명확히 정해 하나씩 매듭짓는 쇠(金)의 깔끔한 습관이 오늘 하루의 만족도를 크게 높여줄 거예요.";
            case WATER -> seed % 2 == 0
                ? "오늘 부족한 물(水)의 유연한 기운을 채우기 위해 수분을 충분히 섭취하고 마음을 느긋하게 열어두세요."
                : "조용한 음악이나 차분한 휴식으로 물(水)의 맑은 에너지를 보충하면 한결 여유롭고 지혜로운 하루가 될 거예요.";
        };
    }

    private List<String> getElementActions(Element neededElement, String grade) {
        return switch (neededElement) {
            case WOOD -> List.of(
                "창문을 활짝 열고 신선한 아침 공기를 마시며 기지개를 켜보세요.",
                "궁금해서 저장만 해뒀던 새로운 장소나 글 하나를 살펴보세요.",
                "가로수길이나 화분이 있는 곳을 지나며 초록빛 풍경을 눈에 담아보세요.",
                "노트나 메모장에 오늘 떠오른 가벼운 아이디어 한 줄을 적어보세요.",
                "평소 가던 길에서 벗어나 작은 산책로를 10분만 걸어보세요."
            );
            case FIRE -> List.of(
                "햇살이 잘 드는 창가나 밝은 조명 아래에서 잠시 여유를 즐겨보세요.",
                "반가운 사람에게 먼저 짧은 안부 메시지나 따뜻한 이모티콘을 보내보세요.",
                "기분이 경쾌해지는 밝은 템포의 음악을 한 곡 선곡해 들어보세요.",
                "거울을 보며 나 자신에게 기분 좋은 미소와 응원을 건네보세요.",
                "평소 아껴두었던 좋아하는 간식이나 디저트로 기분 전환을 해보세요."
            );
            case EARTH -> List.of(
                "든든하고 따뜻한 한 끼 식사를 천천히 음미하며 챙겨 드세요.",
                "가장 편안한 의자나 소파에 기대어 5분간 눈을 감고 호흡을 가다듬어 보세요.",
                "오래 함께해 손때가 묻은 애착 물건을 곁에 두고 하루를 보내보세요.",
                "달콤하고 고소한 음료 한 잔으로 오후의 나른함을 달래보세요.",
                "오늘 하루 무사히 해낸 작은 일 세 가지를 떠올리며 스스로를 칭찬해 주세요."
            );
            case METAL -> List.of(
                "가방 속이나 책상 위에서 쓰지 않는 영수증과 물건 하나를 비워보세요.",
                "오늘 꼭 해야 할 가장 중요한 일 딱 한 가지만 먼저 체크해 끝내보세요.",
                "시원하거나 상쾌한 향기를 맡으며 머릿속을 맑게 환기해 보세요.",
                "자주 쓰는 휴대폰 화면이나 안경을 깨끗하게 닦아 시야를 밝혀보세요.",
                "미뤄두었던 짧은 답장이나 간단한 정리 하나를 깔끔하게 마무리해 보세요."
            );
            case WATER -> List.of(
                "따뜻한 물이나 차를 한 잔 천천히 마시며 몸의 긴장을 풀어보세요.",
                "좋아하는 잔잔한 플레이리스트를 들으며 나만의 휴식 시간을 가져보세요.",
                "잠들기 전 따뜻한 물로 샤워하며 오늘의 피로를 부드럽게 씻어내 보세요.",
                "복잡한 생각이 들 때는 잠시 휴대폰을 내려놓고 창밖을 바라보세요.",
                "내일을 위해 평소보다 30분만 일찍 휴식 준비를 시작해 보세요."
            );
        };
    }

    private List<String> getElementLuckyItems(Element neededElement) {
        return switch (neededElement) {
            case WOOD -> List.of("미니 수첩", "나무 볼펜", "에코백", "네잎클로버 책갈피", "우드 코스터");
            case FIRE -> List.of("하트 키링", "미니 손거울", "아로마 캔들", "선글라스", "립밤");
            case EARTH -> List.of("도자기 머그컵", "가죽 카드지갑", "포근한 손수건", "그립톡", "미니 파우치");
            case METAL -> List.of("메탈 열쇠고리", "손목시계", "실버 반지", "텀블러", "동전지갑");
            case WATER -> List.of("무선 이어폰", "보습 핸드크림", "미니 우산", "유리컵", "향수 공병");
        };
    }

    private List<String> getElementLuckyColors(Element neededElement) {
        return switch (neededElement) {
            case WOOD -> List.of("연두색", "민트색", "올리브그린", "청록색");
            case FIRE -> List.of("코랄핑크", "살구색", "라벤더", "체리레드");
            case EARTH -> List.of("버터옐로우", "크림베이지", "머스타드", "캐러멜브라운");
            case METAL -> List.of("아이보리", "실버그레이", "화이트", "샴페인골드");
            case WATER -> List.of("하늘색", "네이비", "아쿠아블루", "코발트블루");
        };
    }

    private List<String> getCharacterQuotes(String character, String grade) {
        return switch (character) {
            case "mage" -> switch (grade) {
                case "good" -> List.of(
                    "작은 변화도 놓치지 않는 게 마법의 시작이야!",
                    "별들의 흐름이 오늘 네 편을 들어주고 있어!",
                    "반짝이는 영감이 떠오르면 주저하지 말고 펼쳐봐!",
                    "오늘 네가 내딛는 발걸음마다 신비한 행운이 깃들 거야.",
                    "우연처럼 보이는 일 속에 오늘의 마법 열쇠가 숨어 있어!"
                );
                case "normal" -> List.of(
                    "서두르지 않아도 모든 건 가장 알맞은 때가 있는 법이야.",
                    "고요한 호수 위에서 달빛이 가장 또렷하게 보이는 법이지.",
                    "평범한 하루 속에도 자세히 보면 작은 마법이 숨어 있단다.",
                    "네 리듬대로 차분히 주문을 외우듯 하루를 채워가 보자.",
                    "익숙한 길 위에서 새로운 별빛을 발견하게 될지도 몰라."
                );
                default -> List.of(
                    "때로는 가만히 관찰하는 것이 최고의 마법이지.",
                    "구름이 별을 가려도 별이 사라진 건 아니니까 걱정 마.",
                    "마력이 부족할 땐 포션을 마시듯 푹 쉬어 가는 게 정답이야.",
                    "오늘은 바람이 불어오는 방향을 조용히 읽으며 숨을 골라보자.",
                    "잠시 멈춰 서서 지도를 다시 살피면 훨씬 안전한 길이 보일 거야."
                );
            };
            case "healer" -> switch (grade) {
                case "good" -> List.of(
                    "오늘 너의 미소가 주변을 따뜻하게 물들일 거야.",
                    "기분 좋은 바람이 불어오네, 오늘 하루가 참 다정할 거야.",
                    "네가 건넨 작은 친절이 더 큰 행복으로 돌아오는 날이야.",
                    "마음이 활짝 피어나는 날이네, 기쁜 순간들을 마음껏 누려봐!",
                    "오늘 너에게 찾아오는 반가운 행운들을 두 팔 벌려 안아줘."
                );
                case "normal" -> List.of(
                    "오늘은 천천히 가도 괜찮아, 언제나 네 곁에 있을게.",
                    "따뜻한 차 한 잔처럼 포근하고 잔잔한 하루가 되길 바라.",
                    "크게 애쓰지 않아도 지금 그대로 충분히 잘하고 있어.",
                    "소소한 일상 속에서 네 마음이 편안하게 쉬어 갔으면 좋겠어.",
                    "네가 가장 편안해하는 속도가 오늘의 가장 완벽한 속도야."
                );
                default -> List.of(
                    "마음에 쉼표가 필요한 날이야, 절대 무리하지 마.",
                    "조금 느려도 괜찮으니까 오늘은 네 마음을 제일 먼저 토닥여줘.",
                    "지칠 땐 언제든 내 옆에서 잠시 기대어 쉬어도 좋아.",
                    "모든 걸 완벽하게 해내지 않아도 괜찮아, 오늘도 수고 많았어.",
                    "따뜻한 이불처럼 포근한 휴식이 오늘 너에게 가장 좋은 약이야."
                );
            };
            default -> switch (grade) {
                case "good" -> List.of(
                    "작은 발견도 모이면 멋진 모험이 된다고!",
                    "오늘 운세 최고인데? 자신감 있게 앞으로 나아가 보자!",
                    "망설이던 퀘스트가 있다면 오늘이 바로 도전할 타이밍이야!",
                    "발걸음이 가벼운 걸 보니 오늘 멋진 보물을 찾겠는걸!",
                    "네가 가는 길이 곧 행운의 루트야, 힘차게 출발해 볼까!"
                );
                case "normal" -> List.of(
                    "한 걸음씩 가다 보면 어느새 목적지에 도착해 있을걸!",
                    "꾸준히 경험치를 쌓는 날이야말로 진짜 모험가의 기본이지!",
                    "오늘도 내 페이스대로 씩씩하게 하루를 클리어해 보자고!",
                    "평화로운 마을을 산책하듯 가벼운 마음으로 걸어보자!",
                    "작은 퀘스트 하나씩 해결하다 보면 뿌듯한 하루가 될 거야!"
                );
                default -> List.of(
                    "모험도 좋지만 캠프파이어 앞에서 쉬어 가는 날도 필요하지!",
                    "비바람이 불 땐 무리해서 전진하지 말고 베이스캠프에서 재정비하자!",
                    "오늘은 방어력을 높이고 체력을 아끼는 전략으로 가는 거야!",
                    "잠깐 쉬어 간다고 모험이 끝나는 건 아니니까 마음 푹 놓으라고!",
                    "내일 더 힘차게 달리기 위해 오늘은 일찍 장비 내려놓고 푹 쉬자!"
                );
            };
        };
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
