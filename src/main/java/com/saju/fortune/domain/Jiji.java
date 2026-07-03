package com.saju.fortune.domain;

public enum Jiji {
    JA("자", Element.WATER), CHUK("축", Element.EARTH),
    IN("인", Element.WOOD), MYO("묘", Element.WOOD),
    JIN("진", Element.EARTH), SA("사", Element.FIRE),
    O("오", Element.FIRE), MI("미", Element.EARTH),
    SHIN("신", Element.METAL), YU("유", Element.METAL),
    SUL("술", Element.EARTH), HAE("해", Element.WATER);

    private final String name;
    private final Element element;

    Jiji(String name, Element element) { 
        this.name = name; 
        this.element = element; 
    }

    public String getName() { return name; }
    public Element getElement() { return element; }
}