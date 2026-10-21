package com.example.studenthousing.data.model;

public class MatchSuggestion {
    private AccommodationPost mine;
    private AccommodationPost partner;
    private int score;

    public MatchSuggestion(AccommodationPost mine, AccommodationPost partner, int score) {
        this.mine = mine;
        this.partner = partner;
        this.score = score;
    }

    public AccommodationPost getMine() { return mine; }
    public AccommodationPost getPartner() { return partner; }
    public int getScore() { return score; }
}
