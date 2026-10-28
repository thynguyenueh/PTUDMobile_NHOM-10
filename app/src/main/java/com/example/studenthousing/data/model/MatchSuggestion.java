package com.example.studenthousing.data.model;

public class MatchSuggestion {
    private AccommodationPost mine;
    private AccommodationPost partner;
    public MatchSuggestion(AccommodationPost mine, AccommodationPost partner) {
        this.mine = mine;
        this.partner = partner;
    }

    public AccommodationPost getMine() { return mine; }
    public AccommodationPost getPartner() { return partner; }
}
