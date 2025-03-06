package model;

import org.json.JSONObject;

public class Card
{
    private String id;
    private String name;
    private double damage;
    private String packageId;

    public Card(String id, String name, double damage, String packageId)
    {
        this.id = id;
        this.name = name;
        this.damage = damage;
        this.packageId = packageId;
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

    public String getPackageId() { return packageId; }

    public void setPackageId(String packageId) { this.packageId = packageId; }


    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        json.put("Id", id);
        json.put("Name", name);
        json.put("Damage", damage);
        return json;
    }
}