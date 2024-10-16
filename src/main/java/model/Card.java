package model;

public class Card
{
    private String name;
    private int damage;
    private String elementType;

    public Card(String name, int damage, String elementType)
    {
        this.name = name;
        this.damage = damage;
        this.elementType = elementType;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getDamage()
    {
        return damage;
    }

    public void setDamage(int damage)
    {
        this.damage = damage;
    }

    public String getElementType()
    {
        return elementType;
    }

    public void setElementType(String elementType)
    {
        this.elementType = elementType;
    }
}