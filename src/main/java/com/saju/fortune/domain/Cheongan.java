package com.saju.fortune.domain;

public enum Cheongan {
    GAP("갑", Element.WOOD), EUL("을", Element.WOOD),
    BYUNG("병", Element.FIRE), JEONG("정", Element.FIRE),
    MU("무", Element.EARTH), GI("기", Element.EARTH),
    GYUNG("경", Element.METAL), SHIN("신", Element.METAL),
    IM("임", Element.WATER), GYE("계", Element.WATER);

    private final String name;
    private final Element element;

    Cheongan(String name, Element element) { 
        this.name = name; 
        this.element = element; 
    }

    public String getName() { return name; }
    public Element getElement() { return element; }
}