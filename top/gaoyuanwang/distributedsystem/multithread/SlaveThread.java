package top.gaoyuanwang.distributedsystem.multithread;

import top.gaoyuanwang.distributedsystem.game.GameInfo;
import top.gaoyuanwang.distributedsystem.game.GameInfoImpl;
import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfo;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SlaveThread extends Thread{
    ListenerThread listenerThread;

    GameInfo slaveGameInfo;

    TrackerInfo trackerInfo;

    /**
     * Constructor for SlaveThread
     * @param listenerThread
     * @param slaveGameInfo
     * @param trackerInfo
     */
    public SlaveThread(ListenerThread listenerThread, GameInfo slaveGameInfo, TrackerInfo trackerInfo){
        this.listenerThread = listenerThread;
        this.slaveGameInfo = slaveGameInfo;
        this.trackerInfo = trackerInfo;
    }

    /**
     * the slave thread is used to check the other players are alive or not,
     * if not, remove the player from the gameInfo and trackerInfo,
     * this thread will be called every 500ms by the ListenerThread
     */
    public void run(){
        GameInfoImpl gameInfo = null;
        try{
            gameInfo = (GameInfoImpl) listenerThread.gameInfo.getGameInfo();
        } catch (RemoteException e){
            try{
                gameInfo = (GameInfoImpl) slaveGameInfo.getGameInfo();
                Player master = gameInfo.getMaster();
                Player slave = gameInfo.getSlave();
                trackerInfo.removePlayer(master);
                slaveGameInfo.updateMaster(slave);
                listenerThread.runMasterThread(slaveGameInfo, trackerInfo);
            } catch (Exception e1){
                e1.printStackTrace();
            }
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final GameInfoImpl gameInfo1 = gameInfo;
        executorService.submit(() -> {
            for (Player player : new ArrayList<>(gameInfo1.getPlayersAndInfo().keySet())) {
                int masterPort = gameInfo1.getMaster().port;
                int slavePort = gameInfo1.getSlave().port;
                if (player.port != masterPort && player.port != slavePort) {
                    if (!Util.ping(player.port)) {
                        try {
                            trackerInfo.removePlayer(player);
                            listenerThread.gameInfo.removePlayer(player);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
