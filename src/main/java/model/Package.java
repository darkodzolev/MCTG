package model;

import java.util.List;
import java.util.UUID;

public class Package
{
    private List<Card> cards;
    private String id;

    public Package(List<Card> cards, String id)
    {
        this.cards = cards;
        this.id = id;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }






}