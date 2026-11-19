package com.lexoratech.lexorafix.models;

public class OnboardingItem {
    private int image;
    private String emoji;
    private String titleLine1;
    private String titleLine2;
    private String description;

    public OnboardingItem(int image, String emoji, String titleLine1, String titleLine2, String description) {
        this.image = image;
        this.emoji = emoji;
        this.titleLine1 = titleLine1;
        this.titleLine2 = titleLine2;
        this.description = description;
    }

    public int getImage() { return image; }
    public String getEmoji() { return emoji; }
    public String getTitleLine1() { return titleLine1; }
    public String getTitleLine2() { return titleLine2; }
    public String getDescription() { return description; }
}
