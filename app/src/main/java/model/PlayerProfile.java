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
    // Constructor mặc định (cần thiết cho một số thư viện chuyển đổi dữ liệu)
    public PlayerProfile() {}
}