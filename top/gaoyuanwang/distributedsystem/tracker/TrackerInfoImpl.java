package top.gaoyuanwang.distributedsystem.tracker;

import top.gaoyuanwang.distributedsystem.pojo.Player;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class TrackerInfoImpl implements TrackerInfo, Serializable {

    // Size of the maze
    public Integer N;

    // Number of treasures
    public Integer K;

    // List of players
    public List<Player> playerList;

    // Port for new player
    public Integer portForNewPlayer;

    public TrackerInfoImpl(Integer N, Integer K) {
        this.N = N;
        this.K = K;
        this.playerList = new ArrayList<>();
        this.portForNewPlayer = 9200;
    }

    public TrackerInfoImpl(Integer N, Integer K, List<Player> playerList, Integer portForNewPlayer) {
        this.N = N;
        this.K = K;
        this.playerList = playerList;
        this.portForNewPlayer = portForNewPlayer;
    }

    @Override
    public TrackerInfo getTrackerInfo() throws RemoteException {
        return new TrackerInfoImpl(N, K, playerList, portForNewPlayer);
    }

    @Override
    public synchronized void addPlayer(Player p) throws RemoteException {
        p.setPort(portForNewPlayer);
        playerList.add(p);
        portForNewPlayer++;
    }

    @Override
    public synchronized TrackerInfo removePlayer(Player p) throws RemoteException {
        playerList.remove(p);
        return getTrackerInfo();
    }
}
