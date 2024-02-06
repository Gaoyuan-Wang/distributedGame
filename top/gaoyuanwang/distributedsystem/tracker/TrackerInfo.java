package top.gaoyuanwang.distributedsystem.tracker;

import top.gaoyuanwang.distributedsystem.pojo.Player;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TrackerInfo extends Remote{

    /**
     * obtain the trackerInfo
     * @return TrackerInfo
     * @throws RemoteException
     */
    TrackerInfo getTrackerInfo() throws RemoteException;

    /**
     * add player
     * @param player
     * @throws RemoteException
     */
    void addPlayer(Player player) throws RemoteException;

    /**
     * remove player
     * @param player
     * @return TrackerInfo
     * @throws RemoteException
     */
    TrackerInfo removePlayer(Player player) throws RemoteException;
}
