package model;

import java.awt.*;

public class User
{
    private String username;
    private String password;
    private String token;
    private int coins;
    private String bio;
    private String image;

    public User(String username, String password, int coins)
    {
        this.username = username;
        this.password = password;
        this.coins = coins;
    }

    public User(String username, String password, String token, int coins)
    {
        this.username = username;
        this.password = password;
        this.token = null;
        this.coins = 20;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public int getCoins()
    {
        return coins;
    }

    public void setCoins(int coins)
    {
        this.coins = coins;
    }
}