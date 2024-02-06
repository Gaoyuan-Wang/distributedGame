package top.gaoyuanwang.distributedsystem.multithread;

import top.gaoyuanwang.distributedsystem.game.GameInfo;
import top.gaoyuanwang.distributedsystem.game.GameInfoImpl;
import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.pojo.ServerType;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ListenerThread extends Thread{

    private Player player;
    protected GameInfo gameInfo;
    private TrackerInfo trackerInfo;
    private ServerType serverType;
    private ServerSocket socket;
    private ScheduledExecutorService scheduledExecutorService;
    private static final int PING_INTERVAL = 500;

    /**
     * Constructor for ListenerThread, run masterThread and socket
     * @param player
     * @param gameInfo
     * @param trackerInfo
     * @param serverType
     */
    public ListenerThread(Player player, GameInfo gameInfo, TrackerInfo trackerInfo, ServerType serverType){
        this.player = player;
        this.gameInfo = gameInfo;
        this.trackerInfo = trackerInfo;
        this.serverType = serverType;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        if(serverType == ServerType.MASTER){
            runMasterThread(gameInfo);
        }
        try{
            socket = new ServerSocket(player.getPort());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Run thread, set slave thread, accept socket and create requestHandlerThread when it receives a request
     */
    public void run(){
        try {
            if(serverType.equals(ServerType.SLAVE)){
                tryRunSlaveThread(-1);
            }
            while (true) {
                Socket s = socket.accept();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                RequestHandlerThread requestHandlerThread = new RequestHandlerThread(this, in, out);
                requestHandlerThread.start();
                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run master thread for the first time
     * @param gameInfo
     */
    private void runMasterThread(GameInfo gameInfo){
        MasterThread masterThread = new MasterThread(gameInfo, trackerInfo);
        scheduledExecutorService.scheduleAtFixedRate(masterThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Run master thread when master is down, then slave becomes master
     * @param gameInfo
     * @param trackerInfo
     * @throws RemoteException
     */
    public void runMasterThread(GameInfo gameInfo, TrackerInfo trackerInfo) throws RemoteException {
        serverType = ServerType.MASTER;
        GameInfoImpl gameInfoImpl = (GameInfoImpl) gameInfo.getGameInfo();
        GameInfo gameInfoOfMaster = (GameInfo) UnicastRemoteObject.exportObject(gameInfoImpl, 0);

        this.gameInfo = gameInfoOfMaster;
        this.gameInfo.setSlaveGameInfo(gameInfoOfMaster);

        this.trackerInfo = trackerInfo;

        MasterThread masterThread = new MasterThread(gameInfoOfMaster, trackerInfo);
        scheduledExecutorService.shutdown();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(masterThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Run slave thread
     * @param gameInfo
     */
    private void runSlaveThread(GameInfo gameInfo){
        SlaveThread slaveThread = new SlaveThread(this, gameInfo, trackerInfo);
        scheduledExecutorService.scheduleAtFixedRate(slaveThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Try to run slave thread, it gets game info from master, if master is down, it becomes master.
     * this method will call runSlaveThread() or runMasterThread()
     * @param masterPort
     * @throws RemoteException
     */
    protected void tryRunSlaveThread(int masterPort) throws RemoteException {
        if(masterPort != -1){
            try {
                gameInfo = Util.getGameInfoFromSocket(masterPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        GameInfoImpl gameInfoImplOfSlave = (GameInfoImpl) gameInfo.setSlave(player);
        GameInfo gameInfoOfSlave = (GameInfo) UnicastRemoteObject.exportObject(gameInfoImplOfSlave, 0);
        try{
            gameInfo.setSlaveGameInfo(gameInfoOfSlave);
            runSlaveThread(gameInfoOfSlave);
        } catch (RemoteException e){
            Player master = gameInfoImplOfSlave.getMaster();
            Player slave = gameInfoImplOfSlave.getSlave();
            trackerInfo.removePlayer(master);
            gameInfoOfSlave.updateMaster(slave);
            runMasterThread(gameInfoOfSlave, trackerInfo);
        }
        System.out.println("Slave is running");
    }

    public GameInfo getGameInfo(){
        return gameInfo;
    }

    public TrackerInfo getTrackerInfo(){
        return trackerInfo;
    }

    public void setGameInfo(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }
}
