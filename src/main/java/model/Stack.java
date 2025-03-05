package model;

import java.util.ArrayList;
import java.util.List;

public class Stack
{
    private List<Card> cards;

    public Stack()
    {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card)
    {
        cards.add(card);
    }

    public void addCards(List<Card> cardsToAdd)
    {
        cards.addAll(cardsToAdd);
    }

    public boolean removeCard(Card card)
    {
        return cards.remove(card);
    }

    public List<Card> getCards()
    {
        return cards;
    }

    public Card getCardById(String id)
    {
        return cards.stream()
                .filter(card -> card.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}