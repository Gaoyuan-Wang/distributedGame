package top.gaoyuanwang.distributedsystem.pojo;

import java.io.Serializable;

public class Player implements Serializable {

    // Name of the player
    public String name;

    // Port allocated by Tracker
    public Integer port;

    public Player(String name) {
        this.name = name;
    }

    /**
     * Set the port of the player
     * @param port
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the port of the player
     * @return port
     */
    public Integer getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + port;
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) return true;
        if (!(that instanceof Player)) return false;
        Player player = (Player) that;
        return player.name.equals(this.name) && player.port.equals(this.port);
    }

    @Override
    public String toString() {
        return name + "@localhost:" + port;
    }
}
