package model;

import org.json.JSONObject;

public class Card
{
    private String id;
    private String name;
    private float damage;

    public Card(String id, String name, float damage)
    {
        this.id = id;
        this.name = name;
        this.damage = damage;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public float getDamage()
    {
        return damage;
    }

    public void setDamage(int damage)
    {
        this.damage = damage;
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        json.put("Id", id);
        json.put("Name", name);
        json.put("Damage", damage);
        return json;
    }
}