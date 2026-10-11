package com.saju.fortune.dto;

public class SajuRequest {
    private String character;
    private BirthDto birth;

    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private Integer birthTime;
    private String gender;

    public SajuRequest() {}

    public String getCharacter() { return character != null ? character : "warrior"; }
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