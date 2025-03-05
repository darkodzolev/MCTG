package model;

public class User
{
    private String username;
    private String password;
    private String token;
    private int coins;

    public User(String username, String password)
    {
        this.username = username;
        this.password = password;
        this.token = null;
        this.coins = 0;
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