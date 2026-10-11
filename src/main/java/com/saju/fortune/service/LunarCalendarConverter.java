package com.saju.fortune.service;

import com.ibm.icu.util.ChineseCalendar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Component
public class LunarCalendarConverter {

    public LocalDate toSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        try {
            ChineseCalendar cc = new ChineseCalendar();
            cc.clear();
            cc.set(ChineseCalendar.EXTENDED_YEAR, lunarYear + 2637);
            cc.set(ChineseCalendar.MONTH, lunarMonth - 1);
            cc.set(ChineseCalendar.IS_LEAP_MONTH, isLeapMonth ? 1 : 0);
            cc.set(ChineseCalendar.DAY_OF_MONTH, lunarDay);

            Date solarDate = cc.getTime();
            return Instant.ofEpochMilli(solarDate.getTime())
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDate();
        } catch (Exception e) {
            return LocalDate.of(lunarYear, Math.max(1, Math.min(12, lunarMonth)), Math.max(1, Math.min(28, lunarDay)));
        }
    }
}
