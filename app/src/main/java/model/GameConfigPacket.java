package model;
import java.io.Serializable;

public class GameConfigPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int hostColor;
    public String playerName;

    public GameConfigPacket(int color, String name) {
        this.hostColor = color;
        this.playerName = name;
    }
}