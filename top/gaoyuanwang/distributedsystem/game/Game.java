package top.gaoyuanwang.distributedsystem.game;

import top.gaoyuanwang.distributedsystem.multithread.ListenerThread;
import top.gaoyuanwang.distributedsystem.multithread.Util;
import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.pojo.ServerType;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfo;
import top.gaoyuanwang.distributedsystem.tracker.TrackerInfoImpl;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class Game {

    // current player
    private static Player player;

    // RMI registry
    private static Registry registry;

    // Tracker IP
    private static String trackerIP = null;

    // Tracker port
    private static Integer port = 0;

    // GameInfo
    private static GameInfo gameInfo;

    // GameInfoImpl
    private static GameInfoImpl gameInfoImpl;

    // ListenerThread
    private static ListenerThread listenerThread;

    // Observable, for notifying the GUI
    private PropertyChangeSupport observable;

    /**
     * Constructor of the game, in order to initialize the GUI and add the GUI as the observer,
     * `observable` can not be used in a static method
     */
    public Game() {
        GUI gui = new GUI(gameInfoImpl, player.name);
        observable = new PropertyChangeSupport(this);
        observable.addPropertyChangeListener(gui);
    }


    public static void main(String[] args) {
        // get the player name, tracker IP and port from the command line or use the default value
        if(args.length == 3) {
            player = new Player(args[2]);
            trackerIP = args[0];
            port = Integer.valueOf(args[1]);
        } else if (args.length == 0) {
            player = new Player(nameAllocate());
        } else {
            System.out.println("Usage: java Game <tracker-ip> <port> <player-name>");
            System.exit(-1);
        }

        // Get the tracker info from the tracker
        TrackerInfo trackerInfo = null;
        TrackerInfoImpl trackerInfoImpl = null;
        try {
            registry = LocateRegistry.getRegistry(trackerIP, port);
            trackerInfo = (TrackerInfo) registry.lookup("Tracker");
            trackerInfo.addPlayer(player);
            // Remote call require verification for success
            trackerInfoImpl = (TrackerInfoImpl) trackerInfo.getTrackerInfo();
            if(trackerInfoImpl == null) throw new NotBoundException();
        } catch (Exception e) {
            System.err.println("Tracker is not ready.");
            System.exit(-1);
        }

        player.setPort(getPortFromTrackerInfo(trackerInfoImpl.playerList));

        try {
            switch (trackerInfoImpl.playerList.size()) {
                case 1:
                    GameInfoImpl gameInfo1 = new GameInfoImpl(trackerInfoImpl.N, trackerInfoImpl.K);
                    gameInfo1.setMaster(player);
                    gameInfo = (GameInfo) UnicastRemoteObject.exportObject(gameInfo1, 0);
                    gameInfoImpl = gameInfo.addPlayer(player);
                    System.out.println("Game info master: " + player.name);
                    listenerThread = new ListenerThread(player, gameInfo, trackerInfo, ServerType.MASTER);
                    listenerThread.start();
                    break;
                case 2:
                    gameInfo = getGameInfo(trackerInfo, player);
                    gameInfoImpl = tryAddPlayer(gameInfo, trackerInfo, player, 10);
                    System.out.println("Game info slave: " + player.name);
                    listenerThread = new ListenerThread(player, gameInfo, trackerInfo, ServerType.SLAVE);
                    listenerThread.start();
                    break;
                default:
                    gameInfo = getGameInfo(trackerInfo, player);
                    gameInfoImpl = tryAddPlayer(gameInfo, trackerInfo, player, 10);
                    System.out.println("Game info none: " + player.name);
                    listenerThread = new ListenerThread(player, gameInfo, trackerInfo, ServerType.NONE);
                    listenerThread.start();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        Game game = new Game();
        startGame(listenerThread, game, null);
    }


    /**
     * Allocate a random name for the player
     * @return name
     */
    private static String nameAllocate() {
        return "Player" + UUID.randomUUID().toString().replace("-","").substring(0, 16);
    }

    /**
     * Get the allocated port number from the tracker info
     * @return port number
     */
    private static Integer getPortFromTrackerInfo(List<Player> playerList) {
        Integer port = null;
        for(Player p : playerList) {
            if(player.name.equals(p.name)) {
                port = p.getPort();
                break;
            }
        }
        return port;
    }

    /**
     * Get the Gameinfo from the tracker through socket, the GameInfo is a remote object
     * @param trackerInfo
     * @param currentPlayer
     * @return GameInfo
     */
    private static GameInfo getGameInfo(TrackerInfo trackerInfo, Player currentPlayer){
        GameInfo gameInfo = null;
        for(int i = 0;i < 4;i++){
            try{
                TrackerInfoImpl trackerInfoImpl = (TrackerInfoImpl) trackerInfo.getTrackerInfo();
                for(Player player : trackerInfoImpl.playerList){
                    if (!player.equals(currentPlayer)) {
                        try{
                            gameInfo = Util.getGameInfoFromSocket(player.getPort());
                            if (gameInfo != null) return gameInfo;
                        } catch (IOException ioe){
                            if(i == 2) trackerInfo.removePlayer(player);
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return gameInfo;
    }

    /**
     * Try to add the player to the gameInfo, if the game is not ready, recursively try again
     * @param gameInfo
     * @param trackerInfo
     * @param player
     * @param round
     * @return GameInfo
     */
    private static GameInfoImpl tryAddPlayer(GameInfo gameInfo, TrackerInfo trackerInfo, Player player, int round){
        if(round == 0) return null;
        try{
            return gameInfo.addPlayer(player);
        } catch (RemoteException | NullPointerException e){
            gameInfo = getGameInfo(trackerInfo, player);
            return tryAddPlayer(gameInfo, trackerInfo, player, round - 1);
        }
    }

    /**
     * Start the game, if the Master is down, try to get the gameInfo from the slave,
     * if slave is down, try to get the gameInfo through polling base on the tracker info
     * @param listenerThread
     * @param game
     * @param lastUserMove
     */
    private static void startGame(ListenerThread listenerThread, Game game, String lastUserMove){
        GameInfo gameInfo = listenerThread.getGameInfo();
        TrackerInfo trackerInfo = listenerThread.getTrackerInfo();
        try{
            listenUserInput(gameInfo, trackerInfo, game, lastUserMove);
        } catch (GameException e){
            String lastMove = e.userOrder;
            try{
                Player slave = gameInfo.getSlave();
                if(slave != null){
                    gameInfo = Util.getGameInfoFromSocket(slave.getPort());
                    game.updateGameInfo(gameInfo);
                    startGame(listenerThread, game, lastMove);
                }else {
                    throw new IOException("Slave is null");
                }
            } catch (IOException ex) {
                try{
                    gameInfo = listenerThread.getGameInfo();
                    game.updateGameInfo(gameInfo);
                    startGame(listenerThread, game, lastMove);
                } catch (RemoteException e1){
                    try{
                        gameInfo = getGameInfo(listenerThread.getTrackerInfo(), player);
                        listenerThread.setGameInfo(gameInfo);
                        game.updateGameInfo(gameInfo);
                        startGame(listenerThread, game, lastMove);
                    } catch (RemoteException | NullPointerException exc){
                        startGame(listenerThread, game, lastMove);
                    }
                }
            }
        }
    }

    /**
     * Listen to the user input, if the gameInfo is down, throw GameException
     * @param gameInfo
     * @param trackerInfo
     * @param game
     * @param lastMove
     * @throws GameException
     */
    private static void listenUserInput(GameInfo gameInfo, TrackerInfo trackerInfo,Game game, String lastMove) throws GameException{
        Scanner scanner = new Scanner(System.in);
        while(lastMove != null || scanner.hasNext()){
            String order;
            if(lastMove != null) {
                order = lastMove;
                lastMove = null;
            } else {
                order = scanner.nextLine();
            }
            try{
                switch (order) {
                    case "0":
                        game.updateGameInfo(gameInfo);
                        break;
                    case "1":
                        gameInfo.changePlayerPosition(player, -1);
                        game.updateGameInfo(gameInfo);
                        break;
                    case "2":
                        gameInfo.changePlayerPosition(player, gameInfoImpl.N);
                        game.updateGameInfo(gameInfo);
                        break;
                    case "3":
                        gameInfo.changePlayerPosition(player, 1);
                        game.updateGameInfo(gameInfo);
                        break;
                    case "4":
                        gameInfo.changePlayerPosition(player, -gameInfoImpl.N);
                        game.updateGameInfo(gameInfo);
                        break;
                    case "9":
                        gameInfo.removePlayer(player);
                        trackerInfo.removePlayer(player);
                        System.exit(0);
                        break;
                    default:
                        break;
                }
            } catch(RemoteException | NullPointerException e) {
                throw new GameException(order, e);
            }
        }
    }

    /**
     * Update the gameInfo based on user's order and notify the GUI
     * @param gameInfo
     * @throws RemoteException
     */
    private void updateGameInfo(GameInfo gameInfo) throws RemoteException{
        GameInfoImpl gameInfoOfMaster = (GameInfoImpl) gameInfo.getGameInfo();
        if(gameInfoOfMaster != null) gameInfoImpl = gameInfoOfMaster;
        observable.firePropertyChange("gameInfo", null, gameInfoImpl);
    }
}
