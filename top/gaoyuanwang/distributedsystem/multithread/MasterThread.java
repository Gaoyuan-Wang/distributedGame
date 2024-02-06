package top.gaoyuanwang.distributedsystem.multithread;

import top.gaoyuanwang.distributedsystem.game.GameInfo;
import top.gaoyuanwang.distributedsystem.game.GameInfoImpl;
import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfo;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfoImpl;

import java.rmi.RemoteException;
import java.util.List;

public class MasterThread extends Thread{

    private static GameInfo gameInfo;

    private static TrackerInfo trackerInfo;

    /**
     * Constructor for MasterThread
     * @param gameInfo
     * @param trackerInfo
     */
    public MasterThread(GameInfo gameInfo, TrackerInfo trackerInfo){
        try{
            MasterThread.gameInfo = gameInfo;
            MasterThread.trackerInfo = trackerInfo;
            MasterThread.gameInfo.setSlave(null);
            MasterThread.gameInfo.setSlaveGameInfo(null);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * assign slave, it will be called by ListenerThread every 500ms, to make sure the slave is alive
     */
    public void run(){
        try{
            GameInfoImpl gameInfoImpl = (GameInfoImpl) gameInfo.getGameInfo();
            Player master = gameInfoImpl.getMaster();
            Player slave = gameInfoImpl.getSlave();
            if(slave != null){
                boolean result = Util.ping(slave.port);
                if(!result){
                    trackerInfo.removePlayer(slave);
                    gameInfo.removeSlave(slave);
                    tryAssignSlave(master);
                }
            }else {
                tryAssignSlave(master);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Try to assign a slave through polling
     * @param master
     * @throws RemoteException
     */
    private void tryAssignSlave(Player master) throws RemoteException {
        TrackerInfoImpl trackerInfoImpl = (TrackerInfoImpl) trackerInfo.getTrackerInfo();
        List<Player> onlinePlayers = trackerInfoImpl.playerList;
        for(Player player : onlinePlayers){
            if(!player.equals(master) && Util.assignSlave(master.port, player.port)) break;
        }
    }

}
