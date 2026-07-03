package com.saju.fortune.service;

import com.saju.fortune.domain.Cheongan;
import com.saju.fortune.domain.Jiji;
import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuPalja;
import com.saju.fortune.dto.SajuRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class SajuCalculatorEngine {

    private final OpenAiService openAiService;
    private final int[] SOLAR_TERMS = {5, 4, 5, 5, 5, 6, 7, 7, 8, 8, 7, 7};

    public SajuCalculatorEngine(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public FortuneResponseDto getTodayFortune(SajuRequest req) {
        SajuPalja palja = calculatePalja(req);
        return generateFortuneResult(req, palja);
    }

    private SajuPalja calculatePalja(SajuRequest req) {
        SajuPalja palja = new SajuPalja();
        LocalDate birthDate = LocalDate.of(req.getBirthYear(), req.getBirthMonth(), req.getBirthDay());

        int sajuYear = req.getBirthYear();
        if (req.getBirthMonth() == 1 || (req.getBirthMonth() == 2 && req.getBirthDay() < SOLAR_TERMS[1])) {
            sajuYear -= 1;
        }
        int yearCheonganIdx = (sajuYear + 6) % 10;
        int yearJijiIdx = (sajuYear + 8) % 12;
        palja.yearCheongan = Cheongan.values()[yearCheonganIdx];
        palja.yearJiji = Jiji.values()[yearJijiIdx];

        int sajuMonth = req.getBirthMonth();
        if (req.getBirthDay() < SOLAR_TERMS[req.getBirthMonth() - 1]) {
            sajuMonth -= 1;
            if (sajuMonth == 0) sajuMonth = 12;
        }
        int monthJijiIdx = (sajuMonth + 12) % 12;
        palja.monthJiji = Jiji.values()[monthJijiIdx];
        int baseMonthCheonganIdx = (yearCheonganIdx % 5) * 2 + 2; 
        int monthOffset = monthJijiIdx - 2; 
        if (monthOffset < 0) monthOffset += 12;
        palja.monthCheongan = Cheongan.values()[(baseMonthCheonganIdx + monthOffset) % 10];

        LocalDate epochDate = LocalDate.of(2024, 1, 1);
        long daysBetween = ChronoUnit.DAYS.between(epochDate, birthDate);
        int dayOffset = (int) (daysBetween % 60);
        if (dayOffset < 0) dayOffset += 60; 
        palja.dayCheongan = Cheongan.values()[dayOffset % 10];
        palja.dayJiji = Jiji.values()[dayOffset % 12];

        int hourJijiIdx = ((req.getBirthTime() + 1) / 2) % 12;
        palja.hourJiji = Jiji.values()[hourJijiIdx];
        int baseHourCheonganIdx = (palja.dayCheongan.ordinal() % 5) * 2;
        palja.hourCheongan = Cheongan.values()[(baseHourCheonganIdx + hourJijiIdx) % 10];

        return palja;
    }

    private FortuneResponseDto generateFortuneResult(SajuRequest req, SajuPalja palja) {
        LocalDate today = LocalDate.now();
        int todaySeed = today.getDayOfYear();
        int userSeed = palja.dayCheongan.ordinal() + palja.dayJiji.ordinal();
        
        int magicNumber = (todaySeed * userSeed * req.getBirthTime()) % 100;

        String keyword;
        String description;

        if (magicNumber >= 80) {
            keyword = "대길 (大吉)";
        } else if (magicNumber >= 40) {
            keyword = "무난 (無難)";
        } else {
            keyword = "조심 (操心)";
        }

        try {
            description = openAiService.getFortuneDescription(req, palja);
        } catch (Exception e) {
            System.err.println("OpenAI Error: " + e.getMessage());
            e.printStackTrace();
            if (magicNumber >= 80) {
                description = "[AI 서비스 일시 중단 - 기본 운세 대체]\n오늘은 긍정적인 에너지가 가득합니다. 평소 망설이던 일이 있다면 과감하게 도전해 보세요!";
            } else if (magicNumber >= 40) {
                description = "[AI 서비스 일시 중단 - 기본 운세 대체]\n평범하지만 안정적인 하루입니다. 주변 사람들과 따뜻한 말 한마디를 나누어 보세요.";
            } else {
                description = "[AI 서비스 일시 중단 - 기본 운세 대체]\n돌다리도 두들겨 보고 건너야 하는 날입니다. 중요한 결정은 내일로 미루는 것이 좋습니다.";
            }
        }

        return new FortuneResponseDto(palja.getPaljaString(), keyword, magicNumber, description);
    }
}