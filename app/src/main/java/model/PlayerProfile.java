package model;

import java.io.Serializable;

// Trong package model (của cả PC và Android)
public class PlayerProfile implements Serializable {
    public String name;
    public int color;
    public String ip;

    public PlayerProfile(String name, int color, String ip) {
        this.name = name;
        this.color = color;
        this.ip = ip;
    }
}