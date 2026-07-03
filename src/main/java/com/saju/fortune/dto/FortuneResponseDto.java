package com.saju.fortune.dto;

public class FortuneResponseDto {
    private String saju8Letters;
    private String todayKeyword;
    private int luckScore;
    private String description;

    public FortuneResponseDto(String saju8Letters, String todayKeyword, int luckScore, String description) {
        this.saju8Letters = saju8Letters;
        this.todayKeyword = todayKeyword;
        this.luckScore = luckScore;
        this.description = description;
    }

    // Getter
    public String getSaju8Letters() { return saju8Letters; }
    public String getTodayKeyword() { return todayKeyword; }
    public int getLuckScore() { return luckScore; }
    public String getDescription() { return description; }
}