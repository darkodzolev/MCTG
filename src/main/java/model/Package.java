package model;

import java.util.List;
import java.util.UUID;

public class Package
{
    private List<Card> cards;
    private UUID id;

    public Package(List<Card> cards, UUID id)
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }






}