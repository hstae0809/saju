package com.saju.fortune;

import com.saju.fortune.dto.FortuneResponseDto;
import com.saju.fortune.dto.SajuRequest;
import com.saju.fortune.service.LunarCalendarConverter;
import com.saju.fortune.service.SajuCalculatorEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FortuneApplicationTests {

    @Autowired
    private LunarCalendarConverter lunarCalendarConverter;

    @Autowired
    private SajuCalculatorEngine sajuEngine;

    @Test
    void contextLoads() {
    }

    @Test
    void testLunarToSolarConversion() {
        LocalDate solar = lunarCalendarConverter.toSolar(1995, 5, 15, false);
        assertNotNull(solar);
        assertEquals(1995, solar.getYear());
        assertEquals(6, solar.getMonthValue());
        assertEquals(12, solar.getDayOfMonth());
    }

    @Test
    void testLuckyCatFortuneCalculation() {
        SajuRequest req = new SajuRequest();
        req.setCharacter("mage");
        SajuRequest.BirthDto birth = new SajuRequest.BirthDto();
        birth.setYear(1995);
        birth.setMonth(5);
        birth.setDay(15);
        birth.setCalendar("lunar");
        birth.setLeapMonth(false);
        birth.setUnknownTime(true);
        req.setBirth(birth);

        FortuneResponseDto response = sajuEngine.getTodayFortune(req);
        assertNotNull(response);
        assertNotNull(response.getGrade());
        assertNotNull(response.getTitle());
        assertNotNull(response.getQuote());
        assertNotNull(response.getDetails());
        assertFalse(response.getDetails().isEmpty());
    }
}
