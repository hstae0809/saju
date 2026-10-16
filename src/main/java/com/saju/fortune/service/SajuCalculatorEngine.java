package com.saju.fortune.service;

import com.saju.fortune.domain.Cheongan;
import com.saju.fortune.domain.Jiji;
import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuPalja;
import com.saju.fortune.dto.SajuRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class SajuCalculatorEngine {

    private final OpenAiService openAiService;
    private final LunarCalendarConverter lunarCalendarConverter;
    private final int[] SOLAR_TERMS = {5, 4, 5, 5, 5, 6, 7, 7, 8, 8, 7, 7};

    public SajuCalculatorEngine(OpenAiService openAiService, LunarCalendarConverter lunarCalendarConverter) {
        this.openAiService = openAiService;
        this.lunarCalendarConverter = lunarCalendarConverter;
    }

    public FortuneResponseDto getTodayFortune(SajuRequest req) {
        LocalDate solarBirthDate;
        if (req.isLunar()) {
            solarBirthDate = lunarCalendarConverter.toSolar(
                req.getResolvedYear(),
                req.getResolvedMonth(),
                req.getResolvedDay(),
                req.isLeapMonth()
            );
        } else {
            solarBirthDate = LocalDate.of(
                req.getResolvedYear(),
                req.getResolvedMonth(),
                req.getResolvedDay()
            );
        }

        SajuPalja palja = calculatePalja(solarBirthDate, req.getResolvedHour());
        return generateFortuneResult(req, palja);
    }

    private SajuPalja calculatePalja(LocalDate solarDate, int birthHour) {
        SajuPalja palja = new SajuPalja();

        int sajuYear = solarDate.getYear();
        if (solarDate.getMonthValue() == 1 || (solarDate.getMonthValue() == 2 && solarDate.getDayOfMonth() < SOLAR_TERMS[1])) {
            sajuYear -= 1;
        }
        int yearCheonganIdx = (sajuYear + 6) % 10;
        int yearJijiIdx = (sajuYear + 8) % 12;
        palja.yearCheongan = Cheongan.values()[yearCheonganIdx];
        palja.yearJiji = Jiji.values()[yearJijiIdx];

        int sajuMonth = solarDate.getMonthValue();
        if (solarDate.getDayOfMonth() < SOLAR_TERMS[solarDate.getMonthValue() - 1]) {
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
        long daysBetween = ChronoUnit.DAYS.between(epochDate, solarDate);
        int dayOffset = (int) (daysBetween % 60);
        if (dayOffset < 0) dayOffset += 60; 
        palja.dayCheongan = Cheongan.values()[dayOffset % 10];
        palja.dayJiji = Jiji.values()[dayOffset % 12];

        int hourJijiIdx = ((birthHour + 1) / 2) % 12;
        palja.hourJiji = Jiji.values()[hourJijiIdx];
        int baseHourCheonganIdx = (palja.dayCheongan.ordinal() % 5) * 2;
        palja.hourCheongan = Cheongan.values()[(baseHourCheonganIdx + hourJijiIdx) % 10];

        return palja;
    }

    private FortuneResponseDto generateFortuneResult(SajuRequest req, SajuPalja palja) {
        LocalDate today = LocalDate.now();

        int hash = Objects.hash(
            today.getYear(),
            today.getDayOfYear(),
            palja.yearCheongan.ordinal(),
            palja.yearJiji.ordinal(),
            palja.monthCheongan.ordinal(),
            palja.monthJiji.ordinal(),
            palja.dayCheongan.ordinal(),
            palja.dayJiji.ordinal(),
            palja.hourCheongan.ordinal(),
            palja.hourJiji.ordinal(),
            req.getResolvedYear(),
            req.getResolvedMonth(),
            req.getResolvedDay(),
            req.getResolvedHour(),
            req.getCharacter()
        );

        int magicNumber = Math.floorMod(hash, 100);

        String grade;
        if (magicNumber >= 68) {
            grade = "good";
        } else if (magicNumber >= 34) {
            grade = "normal";
        } else {
            grade = "bad";
        }

        return openAiService.getFortune(req, palja, grade, magicNumber);
    }
}