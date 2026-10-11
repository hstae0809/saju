package com.saju.fortune.dto;

import java.util.List;

public class FortuneResponseDto {
    private String grade;
    private String title;
    private String description;
    private List<String> details;
    private String action;
    private String luckyItem;
    private String luckyColor;
    private String quote;
    private String saju8Letters;
    private int luckScore;

    public FortuneResponseDto() {}

    public FortuneResponseDto(String grade, String title, String description, List<String> details,
                              String action, String luckyItem, String luckyColor, String quote,
                              String saju8Letters, int luckScore) {
        this.grade = grade;
        this.title = title;
        this.description = description;
        this.details = details;
        this.action = action;
        this.luckyItem = luckyItem;
        this.luckyColor = luckyColor;
        this.quote = quote;
        this.saju8Letters = saju8Letters;
        this.luckScore = luckScore;
    }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getLuckyItem() { return luckyItem; }
    public void setLuckyItem(String luckyItem) { this.luckyItem = luckyItem; }

    public String getLuckyColor() { return luckyColor; }
    public void setLuckyColor(String luckyColor) { this.luckyColor = luckyColor; }

    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }

    public String getSaju8Letters() { return saju8Letters; }
    public void setSaju8Letters(String saju8Letters) { this.saju8Letters = saju8Letters; }

    public int getLuckScore() { return luckScore; }
    public void setLuckScore(int luckScore) { this.luckScore = luckScore; }
}