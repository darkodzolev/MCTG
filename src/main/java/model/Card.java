package model;

import org.json.JSONObject;

import java.util.UUID;

public class Card
{
    private String id;
    private String name;
    private double damage;
    private UUID packageId;

    public Card(String id, String name, double damage)
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

    public double getDamage()
    {
        return damage;
    }

    public void setDamage(double damage)
    {
        this.damage = damage;
    }

    public UUID getPackageId() { return packageId; }

    public void setPackageId(UUID packageId) { this.packageId = packageId; }


    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        json.put("Id", id);
        json.put("Name", name);
        json.put("Damage", damage);
        return json;
    }
}