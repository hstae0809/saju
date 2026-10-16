package com.saju.fortune.dto;

import java.time.LocalDate;
import java.util.Set;

public class SajuRequest {
    private static final Set<String> ALLOWED_CHARACTERS = Set.of("warrior", "mage", "healer");

    private String character;
    private BirthDto birth;

    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private Integer birthTime;
    private String gender;

    public SajuRequest() {}

    public String getCharacter() {
        if (character == null) return "warrior";
        String normalized = character.trim().toLowerCase();
        return ALLOWED_CHARACTERS.contains(normalized) ? normalized : "warrior";
    }

    public void setCharacter(String character) { this.character = character; }

    public BirthDto getBirth() { return birth; }
    public void setBirth(BirthDto birth) { this.birth = birth; }

    public int getResolvedYear() {
        if (birth != null && birth.getYear() != null) return birth.getYear();
        return birthYear != null ? birthYear : 2000;
    }

    public int getResolvedMonth() {
        if (birth != null && birth.getMonth() != null) return birth.getMonth();
        return birthMonth != null ? birthMonth : 1;
    }

    public int getResolvedDay() {
        if (birth != null && birth.getDay() != null) return birth.getDay();
        return birthDay != null ? birthDay : 1;
    }

    public int getResolvedHour() {
        if (birth != null) {
            if (Boolean.TRUE.equals(birth.getUnknownTime()) || birth.getHour() == null) {
                return 12;
            }
            return birth.getHour();
        }
        return birthTime != null ? birthTime : 12;
    }

    public boolean isLunar() {
        return birth != null && "lunar".equalsIgnoreCase(birth.getCalendar());
    }

    public boolean isLeapMonth() {
        return birth != null && Boolean.TRUE.equals(birth.getLeapMonth());
    }

    public boolean isUnknownTime() {
        return birth != null && Boolean.TRUE.equals(birth.getUnknownTime());
    }

    public void validate() {
        int currentYear = LocalDate.now().getYear();
        int y = getResolvedYear();
        int m = getResolvedMonth();
        int d = getResolvedDay();
        int h = getResolvedHour();

        if (y < 1900 || y > currentYear) {
            throw new IllegalArgumentException("태어난 연도는 1900년부터 " + currentYear + "년 사이여야 합니다.");
        }
        if (m < 1 || m > 12) {
            throw new IllegalArgumentException("태어난 월은 1월부터 12월 사이여야 합니다.");
        }
        int maxDay = isLunar() ? 30 : 31;
        if (d < 1 || d > maxDay) {
            throw new IllegalArgumentException("태어난 일이 올바르지 않습니다.");
        }
        if (!isLunar()) {
            try {
                LocalDate solarDate = LocalDate.of(y, m, d);
                if (solarDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("미래의 날짜는 입력할 수 없습니다.");
                }
            } catch (java.time.DateTimeException e) {
                throw new IllegalArgumentException("존재하지 않는 양력 날짜입니다.");
            }
        }
        if (h < 0 || h > 23) {
            throw new IllegalArgumentException("태어난 시간은 0시부터 23시 사이여야 합니다.");
        }
    }

    public int getBirthYear() { return getResolvedYear(); }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

    public int getBirthMonth() { return getResolvedMonth(); }
    public void setBirthMonth(int birthMonth) { this.birthMonth = birthMonth; }

    public int getBirthDay() { return getResolvedDay(); }
    public void setBirthDay(int birthDay) { this.birthDay = birthDay; }

    public int getBirthTime() { return getResolvedHour(); }
    public void setBirthTime(int birthTime) { this.birthTime = birthTime; }

    public String getGender() { return gender != null ? gender : "M"; }
    public void setGender(String gender) { this.gender = gender; }

    public static class BirthDto {
        private Integer year;
        private Integer month;
        private Integer day;
        private String calendar;
        private Boolean leapMonth;
        private Boolean unknownTime;
        private Integer hour;
        private Integer minute;

        public BirthDto() {}

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }

        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }

        public Integer getDay() { return day; }
        public void setDay(Integer day) { this.day = day; }

        public String getCalendar() { return calendar; }
        public void setCalendar(String calendar) { this.calendar = calendar; }

        public Boolean getLeapMonth() { return leapMonth; }
        public void setLeapMonth(Boolean leapMonth) { this.leapMonth = leapMonth; }

        public Boolean getUnknownTime() { return unknownTime; }
        public void setUnknownTime(Boolean unknownTime) { this.unknownTime = unknownTime; }

        public Integer getHour() { return hour; }
        public void setHour(Integer hour) { this.hour = hour; }

        public Integer getMinute() { return minute; }
        public void setMinute(Integer minute) { this.minute = minute; }
    }
}