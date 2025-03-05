package model;

import java.util.ArrayList;
import java.util.List;

public class Package
{
    private List<Card> cards;

    public Package()
    {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card)
    {
        if (cards.size() < 5)
        {
            cards.add(card);
        }
    }

    public List<Card> getCards()
    {
        return cards;
    }
}