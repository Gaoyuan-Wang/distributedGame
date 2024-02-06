package top.gaoyuanwang.distributedsystem.game;

import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.pojo.PlayerInfo;

import java.rmi.Remote;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface GameInfo extends Remote {
    void syncGameInfoFromMaster(Map<Player, PlayerInfo> playersAndInfo, Set<Integer> treasures, Player master, Player slave) throws RemoteException;

    GameInfoImpl addPlayer(Player player) throws RemoteException;

    GameInfo getGameInfo() throws RemoteException;

    void setMaster(Player master) throws RemoteException;

    GameInfo setSlave(Player slave) throws RemoteException;

    void setSlaveGameInfo(GameInfo slaveGameInfo) throws RemoteException;

    void updateMaster(Player player) throws RemoteException;

    Player getMaster() throws RemoteException;

    Player getSlave() throws RemoteException;

    void removePlayer(Player player) throws RemoteException;

    void changePlayerPosition(Player player, int movement) throws RemoteException;

    void removeSlave(Player slave) throws RemoteException;

    Map<Player, PlayerInfo> getPlayersAndInfo() throws RemoteException;

    Set<Integer> getTreasures() throws RemoteException;

    void allocateTreasures() throws RemoteException;
}
