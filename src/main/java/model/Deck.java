package model;

import java.util.ArrayList;
import java.util.List;

public class Deck
{
    private static final int MAX_CARDS = 4; // Limit deck to 4 cards
    private List<Card> cards;

    public Deck()
    {
        this.cards = new ArrayList<>();
    }

    public boolean addCard(Card card)
    {
        if (cards.size() < MAX_CARDS)
        {
            cards.add(card);
            return true;
        }
        return false; // Deck is full
    }

    public boolean removeCard(Card card)
    {
        return cards.remove(card);
    }

    public List<Card> getCards()
    {
        return cards;
    }

    public void clearDeck()
    {
        cards.clear();
    }
}