package com.saju.fortune.dto;

public class SajuRequest {
    private int birthYear;
    private int birthMonth;
    private int birthDay;
    private int birthTime;
    private String gender;

    public SajuRequest() {}

    // Getter 및 Setter
    public int getBirthYear() { return birthYear; }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }

    public int getBirthMonth() { return birthMonth; }
    public void setBirthMonth(int birthMonth) { this.birthMonth = birthMonth; }

    public int getBirthDay() { return birthDay; }
    public void setBirthDay(int birthDay) { this.birthDay = birthDay; }

    public int getBirthTime() { return birthTime; }
    public void setBirthTime(int birthTime) { this.birthTime = birthTime; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}