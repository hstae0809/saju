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
                "사용자의 사주팔자(8글자), 타고난 일간 기운(%s), 보완이 필요한 오행(%s), 오늘 운세 등급(%s)을 바탕으로 다정한 오늘의 운세를 JSON으로 작성해줘.\n" +
                "한자는 절대 쓰지 말고 순우리말/한글만 사용해. 두루마리 상자 안에 예쁘게 들어가도록 아래 글자 수 제한을 꼭 지켜줘:\n" +
                "{\n" +
                "  \"title\": \"12~14자 이내의 한 줄 제목\",\n" +
                "  \"description\": \"28~34자 이내의 한 줄 총평 요약\",\n" +
                "  \"details\": [\"28~35자 이내의 흐름 설명 문장 1\", \"28~35자 이내의 오늘 조언 문장 2\"],\n" +
                "  \"action\": \"20~25자 이내의 오늘 추천 행동 하나\",\n" +
                "  \"luckyItem\": \"행운 아이템 (단어)\",\n" +
                "  \"luckyColor\": \"행운 컬러 (단어)\",\n" +
                "  \"quote\": \"%s 어조의 20~28자 이내 말풍선 한마디\"\n" +
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
            palja.getPaljaString(),
            req.getResolvedYear(),
            req.getResolvedMonth(),
            req.getResolvedDay(),
            today.getYear(),
            today.getDayOfYear()
        ), 10000);
        int seed2 = Math.floorMod(Objects.hash(
            palja.dayCheongan.ordinal(),
            palja.dayJiji.ordinal(),
            palja.hourCheongan.ordinal(),
            palja.hourJiji.ordinal(),
            magicNumber
        ), 10000);
        int seed3 = Math.floorMod(Objects.hash(
            palja.yearCheongan.ordinal(),
            palja.monthJiji.ordinal(),
            req.getResolvedHour(),
            today.getDayOfMonth()
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
        String quote = quotes.get(seed1 % quotes.size());

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
                {"뜻밖의 반가움이 찾아오는 날", "평소 같으면 지나쳤을 곳에서 작은 즐거움을 발견할지도 몰라요."},
                {"순풍에 돛을 단 듯 가벼운 날", "마음먹었던 일들이 막힘없이 자연스럽게 풀려가는 흐름이에요."},
                {"반짝이는 영감이 찾아오는 날", "스쳐 지나가던 작은 아이디어가 오늘 하루를 특별하게 빛내줄 거예요."},
                {"따뜻한 인연과 웃음이 머무는 날", "주변 사람들과 나누는 다정한 말 한마디에 기분 좋은 행운이 담겨 있어요."},
                {"기다렸던 타이밍이 열리는 날", "망설이던 일이 있다면 오늘 가볍게 첫걸음을 내디뎌도 참 좋아요."},
                {"발걸음마다 행운이 따라붙는 날", "계획하지 않았던 우연한 선택이 오히려 더 멋진 결과를 가져다줄 거예요."}
            };
        } else if ("normal".equals(grade)) {
            return new String[][] {
                {"평범함 속 작은 행복을 찾는 날", "오늘은 소소한 일상이 마음을 잔잔하고 편안하게 해줄 거예요."},
                {"나만의 리듬으로 걸어가는 날", "남들과 속도를 맞추기보다 내 호흡대로 움직일 때 가장 빛나는 하루예요."},
                {"익숙한 풍경이 새로워지는 날", "매일 지나던 길이나 일상 속에서 소소한 여유와 기쁨을 찾기 좋아요."},
                {"차분하게 에너지를 모으는 날", "밀려 있던 작은 일들을 하나씩 정리하면 마음까지 한결 가벼워질 거예요."},
                {"잔잔한 호수처럼 평온한 하루", "크게 무리하지 않아도 계획한 만큼 안정적으로 하루가 흘러갈 거예요."},
                {"소소한 취향과 휴식이 빛나는 날", "내가 좋아하는 작은 습관 하나가 오늘 하루의 든든한 힘이 되어줄 거예요."}
            };
        } else {
            return new String[][] {
                {"조금은 신중하게 걸어야 하는 날", "생각했던 것과 다른 일이 생겨도 조급해하지 마세요."},
                {"마음에 잠시 쉼표를 찍어줄 날", "모든 걸 오늘 다 해내려 하기보다 한 템포 쉬어 가는 여유가 필요해요."},
                {"비 온 뒤 땅이 단단해지는 날", "사소한 변수가 생겨도 당황하지 않고 천천히 살피면 금방 지나갈 거예요."},
                {"나를 먼저 다독여주어야 하는 날", "주변 시선이나 조급한 마음은 잠시 내려놓고 내 컨디션을 먼저 챙겨보세요."},
                {"서두름 대신 여유가 필요한 날", "중요한 결정이나 약속은 한 번 더 확인하고 천천히 움직이는 것이 좋아요."},
                {"내일을 위해 숨을 고르는 하루", "오늘은 에너지를 아끼며 일찍 휴식을 취하는 것만으로도 충분한 하루예요."}
            };
        }
    }

    private String getDayElementDetail(Element dayElement, String grade, int slot) {
        String[] pool = switch (dayElement) {
            case WOOD -> switch (grade) {
                case "good" -> new String[] {
                    "타고난 나무 기운이 활짝 피어나 새로운 시도에 활력을 불어넣어 줘요.",
                    "곧게 뻗는 나무 기운 덕분에 평소 궁금했던 일을 시작하기에 참 좋아요.",
                    "푸른 새싹 같은 에너지가 넘쳐서 작은 도전도 기분 좋게 풀려갈 거예요.",
                    "생기 넘치는 나무 기운이 오늘의 운과 만나 뜻밖의 좋은 기회를 불러와요.",
                    "자라나는 나무처럼 내 안의 가능성이 기분 좋게 뻗어 나가는 하루예요.",
                    "망설이던 일이 있다면 가볍게 한 걸음 내디딜수록 행운이 커질 거예요."
                };
                case "normal" -> new String[] {
                    "차분한 나무 기운이 머물러 서두르지 않고 할 일을 이어가기 좋은 날이에요.",
                    "유연한 나무 기운 덕분에 예상치 못한 상황도 부드럽게 넘길 수 있어요.",
                    "꾸준히 자라는 나무처럼 내 속도를 지키면 마음이 한결 편안해질 거예요.",
                    "익숙한 일상 속에서도 푸른 나무처럼 산뜻한 기분을 발견할 수 있어요.",
                    "욕심내어 빨리 가기보다 한 걸음씩 차근차근 나아가기에 알맞은 날이에요.",
                    "잔잔한 바람에 흔들리는 나뭇잎처럼 여유로운 마음가짐이 행운을 불러와요."
                };
                default -> new String[] {
                    "오늘은 가지를 넓히기보다 내 마음의 뿌리를 단단히 다지는 데 집중해 보세요.",
                    "의욕이 앞서면 쉽게 피로해질 수 있으니 무리한 계획은 잠시 미뤄두세요.",
                    "바람이 부는 날엔 나무도 잠시 몸을 낮추듯 여유 있게 쉬어 가는 게 좋아요.",
                    "한꺼번에 많은 일을 벌이기보다 지금 눈앞의 일만 가볍게 챙겨보세요.",
                    "중요한 결정을 서두르기보다 한 템포 천천히 살피면 마음이 편해질 거예요.",
                    "충분한 휴식으로 지친 가지에 새로운 에너지를 채워주는 하루를 보내세요."
                };
            };
            case FIRE -> switch (grade) {
                case "good" -> new String[] {
                    "타고난 불 기운이 환하게 빛나 주변 사람들에게도 좋은 인상을 남겨요.",
                    "따뜻한 열정의 기운이 오늘의 흐름과 만나 추진력이 한층 돋보일 거예요.",
                    "밝은 햇살 같은 에너지가 가득해 어디서든 기분 좋은 주목을 받는 날이에요.",
                    "마음속 반짝이는 아이디어를 밖으로 표현할수록 즐거운 일이 생겨날 거예요.",
                    "다정한 웃음과 활기찬 태도가 뜻밖의 반가운 행운을 끌어당기는 하루예요.",
                    "평소 해보고 싶었던 일에 자신감 있게 다가서면 멋진 결과가 기다리고 있어요."
                };
                case "normal" -> new String[] {
                    "은은한 온기가 유지되어 마음을 편히 먹으면 일상이 순조롭게 풀려가요.",
                    "밝은 기운을 차분한 일상에 녹여내면 소소하지만 확실한 기쁨을 느껴요.",
                    "모닥불처럼 포근하고 잔잔한 열기가 오늘 하루를 따뜻하게 채워줄 거예요.",
                    "들뜨기보다 차분한 미소로 주변을 대하면 평온하고 기분 좋은 날이 돼요.",
                    "서두르지 않고 내 안의 따뜻한 에너지를 천천히 나누기에 좋은 흐름이에요.",
                    "익숙한 사람들과 소소한 대화를 나누며 마음의 온기를 채워보세요."
                };
                default -> new String[] {
                    "마음이 조금 급해질 수 있으니 성급한 판단은 잠시 미뤄두는 것이 좋아요.",
                    "마음의 열기가 과열되지 않도록 깊게 호흡하며 차분한 여유를 가져보세요.",
                    "감정이 앞서기 쉬운 날이니 한 걸음 물러서서 바라보면 훨씬 편안해져요.",
                    "모든 일에 에너지를 쏟기보다 내 마음의 불씨를 아껴두는 지혜가 필요해요.",
                    "예상과 다른 상황이 생겨도 화내기보다 가볍게 웃어넘기면 금방 지나가요.",
                    "조급함을 내려놓고 조용한 휴식으로 마음의 온도를 편안하게 낮춰보세요."
                };
            };
            case EARTH -> switch (grade) {
                case "good" -> new String[] {
                    "타고난 흙의 든든한 기운이 빛을 발해 신뢰와 행운이 자연스레 모여들어요.",
                    "단단한 대지의 기운이 안정감을 주어 그동안의 노력이 좋은 결실을 맺어요.",
                    "포근한 흙 기운이 주변을 감싸주어 사람들과 함께할수록 기쁨이 배가 돼요.",
                    "차곡차곡 쌓아온 성실함이 오늘 뜻밖의 칭찬과 반가운 소식으로 돌아와요.",
                    "흔들림 없는 편안한 마음이 오늘 하루의 가장 큰 행운 열쇠가 되어줄 거예요.",
                    "믿음직한 태도 덕분에 어떤 일을 맡아도 기분 좋게 술술 풀려나가는 날이에요."
                };
                case "normal" -> new String[] {
                    "묵직하고 평온한 기운이 중심을 잡아주어 나만의 속도를 지키기 좋아요.",
                    "익숙한 공간과 사람들 속에서 마음이 한결 편안하고 든든해질 거예요.",
                    "서두르지 않고 평소 하던 대로만 움직여도 충분히 만족스러운 하루가 돼요.",
                    "넓은 들판처럼 여유로운 마음으로 일상의 작은 행복을 천천히 음미해 보세요.",
                    "크게 변화하려 애쓰지 않아도 지금 있는 자리에서 안정적인 기쁨을 찾아요.",
                    "차분하고 규칙적인 일상이 오늘 내 마음을 가장 포근하게 지켜줄 거예요."
                };
                default -> new String[] {
                    "너무 많은 책임을 혼자 떠안기보다 마음의 짐을 조금 덜어내 보세요.",
                    "한 가지 방법만 고집하기보다 유연하게 생각을 열면 흐름이 가벼워져요.",
                    "마음이 무겁게 느껴질 땐 잠시 멈춰 서서 나를 먼저 다독여 주는 게 좋아요.",
                    "남의 일까지 다 챙기려 하기보다 오늘은 내 편안함을 최우선으로 두세요.",
                    "답답한 일이 생겨도 조급해하지 말고 시간에 맡기면 자연스럽게 풀릴 거예요.",
                    "익숙하고 포근한 곳에서 일찍 쉬며 내일을 위한 에너지를 채워보세요."
                };
            };
            case METAL -> switch (grade) {
                case "good" -> new String[] {
                    "타고난 쇠의 명쾌한 결단력이 빛을 발해 복잡한 일도 시원하게 해결돼요.",
                    "보석처럼 단단한 기운이 강해져 집중력과 판단력이 최고조에 이르는 날이에요.",
                    "깔끔하고 똑 부러지는 일 처리가 주변의 감탄과 좋은 행운을 불러올 거예요.",
                    "고민하던 선택지가 있었다면 오늘의 직관을 믿고 결정해도 아주 좋아요.",
                    "반짝이는 원석처럼 내 장점이 또렷하게 드러나 기분 좋은 성취를 맛봐요.",
                    "불필요한 고민을 덜어낼수록 정말 원하던 행운이 선명하게 다가올 거예요."
                };
                case "normal" -> new String[] {
                    "정돈된 기운이 흐르고 있어 주변이나 생각을 깔끔하게 정리하기 좋아요.",
                    "타고난 분별력 덕분에 불필요한 일에 흔들리지 않고 알찬 하루를 보내요.",
                    "차분하고 맑은 정신으로 해야 할 일을 하나씩 기분 좋게 마칠 수 있어요.",
                    "복잡한 일보다는 단순하고 명확한 일상에 집중할 때 마음이 가장 편해요.",
                    "내 기준과 속도를 차분히 지키며 담백하고 여유로운 하루를 만들어 보세요.",
                    "작은 정돈 하나가 머릿속까지 맑고 상쾌하게 바꿔주는 평온한 날이에요."
                };
                default -> new String[] {
                    "나 자신이나 타인에게 너무 엄격한 잣대를 대기보다 조금 너그러워져 보세요.",
                    "완벽하려는 부담은 내려놓고 조금 빈틈이 있어도 괜찮다고 다독여 주세요.",
                    "날카로운 말이나 성급한 결론은 피하고 부드러운 여유를 갖는 것이 좋아요.",
                    "계획대로 되지 않는 일이 있어도 스스로를 탓하지 말고 편하게 넘겨보세요.",
                    "오늘은 옳고 그름을 따지기보다 내 마음의 평화를 지키는 게 가장 중요해요.",
                    "긴장했던 어깨의 힘을 빼고 따뜻한 휴식으로 하루를 부드럽게 마무리하세요."
                };
            };
            case WATER -> switch (grade) {
                case "good" -> new String[] {
                    "타고난 물의 지혜로운 기운이 흘러 막혔던 일도 자연스럽게 풀려나가요.",
                    "맑은 물처럼 직관력이 반짝여 좋은 아이디어와 반가운 소식을 빠르게 만나요.",
                    "유연하고 부드러운 태도가 어디서든 환영받으며 즐거운 인연을 만들어 줘요.",
                    "흐르는 시냇물처럼 기분 좋은 행운이 일상 곳곳으로 스며드는 날이에요.",
                    "평소 상상만 하던 재미있는 생각을 가볍게 실행에 옮기면 즐거움이 커져요.",
                    "풍부한 감수성과 재치가 빛을 발해 주변 분위기까지 산뜻하게 이끌어 줘요."
                };
                case "normal" -> new String[] {
                    "잔잔하고 깊은 물 기운이 감싸주어 나만의 시간에 조용히 몰두하기 좋아요.",
                    "흐르는 물처럼 자연스럽게 어울리면 큰 힘을 안 들여도 평온한 하루가 돼요.",
                    "고요한 호수 같은 평화로움이 머물러 책을 읽거나 사색하기에 참 좋아요.",
                    "억지로 애쓰지 않아도 물 흐르듯 자연스럽게 하루의 일과가 이어질 거예요.",
                    "조용한 취미나 음악과 함께할 때 내면의 에너지가 기분 좋게 채워져요.",
                    "서두르지 않고 유연한 마음으로 하루를 대하면 소소한 행복이 찾아와요."
                };
                default -> new String[] {
                    "생각이 깊어질 수 있는 날이니 일어나지 않은 일로 미리 걱정하지 마세요.",
                    "마음이 복잡할 때는 억지로 답을 찾기보다 따뜻한 곳에서 몸을 녹여보세요.",
                    "감정의 물결이 흔들릴 땐 중요한 결정을 내일로 미루고 푹 쉬는 게 좋아요.",
                    "혼자 너무 많은 고민에 빠지기보다 가벼운 산책으로 기분을 환기해 보세요.",
                    "차분하고 따뜻한 음료 한 잔으로 복잡한 머릿속을 잠시 비워주는 날이에요.",
                    "오늘은 에너지를 아끼며 포근한 이불 속에서 일찍 하루를 마무리해 보세요."
                };
            };
        };
        return pool[slot % pool.length];
    }

    private String getNeededElementAdvice(Element neededElement, String grade, int slot) {
        String[] pool = switch (neededElement) {
            case WOOD -> new String[] {
                "푸른 식물이나 신선한 바깥 공기를 가까이하면 기분 좋은 생기가 찾아와요.",
                "거창한 도전보다 작은 호기심을 따라가면 훨씬 즐거운 하루가 만들어질 거예요.",
                "잠시 창문을 열어 환기를 시키거나 가벼운 스트레칭으로 활력을 더해 보세요.",
                "새로운 음악이나 가보지 않았던 작은 길 하나가 오늘의 좋은 자극이 돼요.",
                "초록빛 풍경을 눈에 담으며 걸으면 답답했던 마음도 산뜻하게 풀릴 거예요.",
                "떠오르는 작은 아이디어를 메모해 두면 기분 좋은 시작의 씨앗이 될 거예요."
            };
            case FIRE -> new String[] {
                "밝은 표정과 다정한 인사 한마디가 오늘의 행운을 부르는 열쇠가 될 거예요.",
                "마음속 작은 설렘을 따라 기분 전환이 되는 즐거운 일을 하나 시도해 보세요.",
                "따스한 햇살을 쬐거나 밝은 조명 아래에서 기분 좋은 에너지를 채워보세요.",
                "좋아하는 사람에게 먼저 건네는 따뜻한 안부가 하루를 환하게 밝혀줄 거예요.",
                "경쾌한 음악이나 기분 좋은 웃음으로 일상에 작은 활기를 불어넣어 보세요.",
                "스스로에게 건네는 작은 칭찬 한마디가 오늘의 자신감을 활짝 피워줄 거예요."
            };
            case EARTH -> new String[] {
                "서두르지 말고 여유로운 식사와 포근한 휴식을 챙기면 행운이 머물 거예요.",
                "편안하고 익숙한 공간에서 마음의 중심을 차분히 가다듬어 보는 것이 좋아요.",
                "따뜻하고 든든한 한 끼를 챙겨 먹으며 내 몸과 마음을 먼저 아껴주세요.",
                "오래 손때 묻은 애착 물건이나 편안한 옷차림이 오늘의 안정감을 높여줘요.",
                "잠시 눈을 감고 깊게 호흡하며 내 페이스를 천천히 되찾아 보는 건 어떨까요.",
                "소소하고 확실한 일상 루틴을 지키는 것만으로도 마음이 든든해질 거예요."
            };
            case METAL -> new String[] {
                "가방이나 책상 위를 가볍게 정돈하면 막혔던 흐름도 산뜻하게 트일 거예요.",
                "우선순위를 정해 작은 일부터 하나씩 매듭지으면 하루의 만족도가 높아져요.",
                "쓰지 않는 물건이나 복잡한 생각을 하나만 비워내도 마음이 한결 가벼워져요.",
                "상쾌한 공기로 주변을 환기하고 깔끔한 기분으로 오늘 하루를 이어가 보세요.",
                "해야 할 일 중 가장 간단한 것 하나만 먼저 끝내도 기분 좋은 탄력이 붙어요.",
                "자주 쓰는 소지품을 깨끗이 닦아두면 반짝이는 행운이 함께 따라올 거예요."
            };
            case WATER -> new String[] {
                "따뜻한 물이나 차를 자주 마시고 마음을 느긋하게 열어두면 한결 편안해져요.",
                "충분한 휴식과 잔잔한 여유로 에너지를 채우며 내일을 기분 좋게 준비해 보세요.",
                "잔잔한 선율의 음악을 들으며 나만의 조용한 힐링 타임을 꼭 챙겨보세요.",
                "일정을 너무 빽빽하게 채우기보다 중간중간 짧은 쉼표를 넣어주는 게 좋아요.",
                "흐르는 물처럼 유연하게 생각하면 사소한 문제도 자연스럽게 풀려갈 거예요.",
                "오늘 밤에는 따뜻한 샤워와 함께 일찍 긴장을 풀고 포근한 잠을 청해 보세요."
            };
        };
        return pool[slot % pool.length];
    }

    private List<String> getElementActions(Element neededElement, String grade) {
        return switch (neededElement) {
            case WOOD -> List.of(
                "창문을 열고 신선한 공기를 마시며 기지개를 켜보세요.",
                "궁금해서 저장만 해뒀던 것 하나를 가볍게 해보세요.",
                "초록빛 나무나 화분이 있는 길을 잠깐 걸어보세요.",
                "메모장에 오늘 떠오른 기분 좋은 생각을 적어보세요.",
                "평소 가던 길에서 벗어나 짧은 산책을 즐겨보세요.",
                "좋아하는 책이나 글의 한 페이지를 가볍게 읽어보세요."
            );
            case FIRE -> List.of(
                "햇살이 드는 창가에서 잠시 여유를 즐겨보세요.",
                "반가운 사람에게 짧은 안부 메시지를 보내보세요.",
                "기분이 밝아지는 경쾌한 음악을 한 곡 들어보세요.",
                "거울을 보며 나에게 기분 좋은 미소를 건네보세요.",
                "좋아하는 달콤한 간식으로 기분 전환을 해보세요.",
                "기분 좋은 향기가 나는 곳에서 잠시 숨을 골라보세요."
            );
            case EARTH -> List.of(
                "따뜻하고 든든한 식사를 천천히 챙겨 드세요.",
                "편안한 의자에 기대어 5분간 호흡을 가다듬어 보세요.",
                "아끼는 애착 물건을 곁에 두고 하루를 보내보세요.",
                "고소하고 따뜻한 음료로 오후의 여유를 즐겨보세요.",
                "오늘 해낸 작은 일 세 가지를 떠올려 칭찬해 주세요.",
                "가장 편안한 자세로 어깨와 목의 긴장을 풀어보세요."
            );
            case METAL -> List.of(
                "책상 위 쓰지 않는 물건 하나를 깔끔히 정리해 보세요.",
                "가장 중요한 일 딱 한 가지만 먼저 체크해 끝내보세요.",
                "상쾌한 향기를 맡으며 머릿속을 맑게 환기해 보세요.",
                "휴대폰 화면이나 안경을 깨끗하게 닦아보세요.",
                "미뤄두었던 짧은 답장 하나를 가볍게 마쳐보세요.",
                "가방 속 영수증이나 작은 소지품을 가볍게 정돈해 보세요."
            );
            case WATER -> List.of(
                "따뜻한 음료를 마시며 잠깐 쉬어보세요.",
                "좋아하는 음악을 들으며 천천히 산책해 보세요.",
                "따뜻한 물로 샤워하며 오늘의 피로를 풀어보세요.",
                "잠시 휴대폰을 내려놓고 창밖 풍경을 바라보세요.",
                "내일을 위해 평소보다 30분 일찍 휴식을 시작해 보세요.",
                "미지근한 물 한 잔을 천천히 마시며 여유를 가져보세요."
            );
        };
    }

    private List<String> getElementLuckyItems(Element neededElement) {
        return switch (neededElement) {
            case WOOD -> List.of("미니 수첩", "나무 볼펜", "에코백", "네잎클로버 책갈피", "우드 코스터", "초록 엽서");
            case FIRE -> List.of("하트 키링", "미니 손거울", "아로마 캔들", "선글라스", "립밤", "별 모양 스티커");
            case EARTH -> List.of("도자기 머그컵", "가죽 카드지갑", "포근한 손수건", "그립톡", "미니 파우치", "무릎담요");
            case METAL -> List.of("열쇠고리", "손목시계", "실버 반지", "텀블러", "동전지갑", "메탈 클립");
            case WATER -> List.of("무선 이어폰", "보습 핸드크림", "미니 우산", "유리컵", "향수 공병", "물병");
        };
    }

    private List<String> getElementLuckyColors(Element neededElement) {
        return switch (neededElement) {
            case WOOD -> List.of("연두색", "민트색", "올리브그린", "청록색", "피스타치오", "포레스트그린");
            case FIRE -> List.of("코랄핑크", "살구색", "라벤더", "체리레드", "피치핑크", "로즈골드");
            case EARTH -> List.of("버터옐로우", "크림베이지", "머스타드", "캐러멜브라운", "오트밀색", "샌드베이지");
            case METAL -> List.of("아이보리", "실버그레이", "화이트", "샴페인골드", "라이트그레이", "펄화이트");
            case WATER -> List.of("하늘색", "네이비", "아쿠아블루", "코발트블루", "파스텔블루", "보라색");
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
