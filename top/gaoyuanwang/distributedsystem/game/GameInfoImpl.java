package top.gaoyuanwang.distributedsystem.game;

import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.pojo.PlayerInfo;
import top.gaoyuanwang.distributedsystem.pojo.ServerType;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Record the Game State, exported as a remote object, this remote object will not be bound with RMI registry in order to
 * satisfy the assignment requirement. So it can only be requested by socket connection.
 * But the every movement of player will be updated to the GameInfo through its synchronized methods
 * instead of using socket continuously as it is a remote object, which can enhance the efficiency.
 */
public class GameInfoImpl implements GameInfo, Serializable {

    public final Integer N; // Size of the maze

    public final Integer K; // Number of treasures

    private Map<Player, PlayerInfo> playersAndInfo = new HashMap<>(); // List of players and their position and score

    private Set<Integer> treasures = new HashSet<>(); // Set of treasures position

    private Player master; // Master player

    private Player slave; // Slave player

    private GameInfo slaveGameInfo; // Slave GameInfo

    public GameInfoImpl(Integer n, Integer k) {
        N = n;
        K = k;
        allocateTreasures();
    }

    public GameInfoImpl(Integer n, Integer k, Map<Player, PlayerInfo> playersAndInfo, Set<Integer> treasures, Player master, Player slave, GameInfo slaveGameInfo) {
        N = n;
        K = k;
        this.playersAndInfo = playersAndInfo;
        this.treasures = treasures;
        this.master = master;
        this.slave = slave;
        this.slaveGameInfo = slaveGameInfo;
    }

    /**
     * allocate Treasures randomly and sync the info to slave
     */
    public synchronized void allocateTreasures() {
        Random random = new Random();
        while (treasures.size() < K) {
            treasures.add(random.nextInt(N * N));
        }
        syncGameInfoToSlave();
    }

    /**
     * remotely add a player to the game, allocate a position for the player and sync the info to slave
     * @param player
     * @return GameInfoImpl
     */
    public synchronized GameInfoImpl addPlayer(Player player) {
        allocatePosition(player);
        syncGameInfoToSlave();
        return new GameInfoImpl(N, K, playersAndInfo, treasures, master, slave, slaveGameInfo);
    }

    /**
     * allocate a position for the player
     * @param player
     */
    private void allocatePosition(Player player) {
        Set<Integer> currentPlayersPosition = playersAndInfo.values()
                .stream().map(playerInfo -> playerInfo.position).collect(Collectors.toSet());
        Random random = new Random();
        int position = random.nextInt(N * N);
        while (currentPlayersPosition.contains(position) || treasures.contains(position)) {
            position = random.nextInt(N * N);
        }
        playersAndInfo.put(player, new PlayerInfo(position));
    }

    /**
     * set the master and sync the info to slave
     * @param master
     */
    public synchronized void setMaster(Player master) {
        this.master = master;
        syncGameInfoToSlave();
    }

    /**
     * set the slave
     * @param slave
     * @return GameInfo
     */
    public synchronized GameInfo setSlave(Player slave) {
        this.slave = slave;
        return getGameInfo();
    }

    /**
     * remove the slave
     * @param slave
     */
    public synchronized void removeSlave(Player slave){
        playersAndInfo.remove(slave);
        this.slaveGameInfo = null;
        this.slave = null;
    }

    /**
     * sync the info to slave, this method will be called by master
     */
    public synchronized void syncGameInfoToSlave() {
        if(slave != null && slaveGameInfo != null) {
            try{
                slaveGameInfo.syncGameInfoFromMaster(playersAndInfo, treasures, master, slave);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * sync the info from master, this method will be called by slave
     * @param playersAndInfo
     * @param treasures
     * @param master
     * @param slave
     */
    public void syncGameInfoFromMaster(Map<Player, PlayerInfo> playersAndInfo, Set<Integer> treasures, Player master, Player slave) {
        this.playersAndInfo = playersAndInfo;
        this.treasures = treasures;
        this.master = master;
        this.slave = slave;
    }

    /**
     * get the GameInfoImpl
     * @return GameInfoImpl
     */
    public GameInfo getGameInfo() {
        return new GameInfoImpl(N, K, playersAndInfo, treasures, master, slave, slaveGameInfo);
    }

    /**
     * get the master
     * @return Player
     */
    public Player getMaster() {
        return master;
    }

    /**
     * get the slave
     * @return Player
     */
    public Player getSlave() {
        return slave;
    }

    /**
     * get the players and its score and position
     * @return Map<Player, PlayerInfo>
     */
    public Map<Player, PlayerInfo> getPlayersAndInfo() {
        return playersAndInfo;
    }

    /**
     * get the treasures position
     * @return Set<Integer>
     */
    public Set<Integer> getTreasures() {
        return treasures;
    }

    /**
     * set the slaveGameInfo
     */
    public void setSlaveGameInfo(GameInfo slaveGameInfo) {
        this.slaveGameInfo = slaveGameInfo;
    }

    /**
     * update the master, it is called when master is down and the slave will become the master
     * @param player
     */
    public synchronized void updateMaster(Player player){
        this.playersAndInfo.remove(master);
        this.master = player;
        this.slave = null;
        //this.slaveGameInfo = null;
    }

    /**
     * remove the player from the game and sync the info to slave
     * @param player
     */
    public synchronized void removePlayer(Player player){
        playersAndInfo.remove(player);
        syncGameInfoToSlave();
    }

    /**
     * change the player position and sync the info to slave
     * @param player
     * @param movement
     */
    public synchronized void changePlayerPosition(Player player, int movement){
        PlayerInfo playerInfo = playersAndInfo.get(player);
        if ((movement == -1  && playerInfo.position % N == 0) || (movement == N && playerInfo.position >= N * (N - 1)) ||
                (movement == 1 && playerInfo.position % N == N - 1) || (movement == -N && playerInfo.position < N))
            return;
        int newPosition = playerInfo.position + movement;
        for(Map.Entry<Player, PlayerInfo> entry : playersAndInfo.entrySet()){
            if(entry.getValue().position == newPosition) return;
        }

        playerInfo.position = newPosition;
        if(treasures.contains(playerInfo.position)) {
            treasures.remove(playerInfo.position);
            playerInfo.score++;
            allocateTreasures();
        } else {
            syncGameInfoToSlave();
        }
    }
}
