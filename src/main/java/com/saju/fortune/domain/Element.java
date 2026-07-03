package com.saju.fortune.domain;

public enum Element {
    WOOD("나무"), 
    FIRE("불"), 
    EARTH("흙"), 
    METAL("쇠"), 
    WATER("물");

    private final String korean;

    Element(String korean) { 
        this.korean = korean; 
    }

    public String getKorean() { 
        return korean; 
    }
}