package top.gaoyuanwang.distributedsystem.pojo;

import java.io.Serializable;

public class PlayerInfo implements Serializable {

    public Integer position;

    public Integer score;

    /**
     * Constructor, record the position and score of a player
     * @param position
     */
    public PlayerInfo(Integer position) {
        this.position = position;
        this.score = 0;
    }
}
