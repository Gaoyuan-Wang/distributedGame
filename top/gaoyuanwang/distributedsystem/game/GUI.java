package top.gaoyuanwang.distributedsystem.game;

import top.gaoyuanwang.distributedsystem.pojo.Player;
import top.gaoyuanwang.distributedsystem.pojo.PlayerInfo;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

public class GUI extends JFrame implements PropertyChangeListener {

    private String playerName; // the name of the player
    private final JLabel[][] map; // the map of maze
    private final JLabel[] info; // the master, slave, and the info of players
    private GameInfoImpl gameInfoImpl;
    private static final int MAX_ROWS = 20;
    public GUI(GameInfoImpl gameInfoImpl, String name) {
        setVisible(true);
        int rows = gameInfoImpl.N;
        int columns = gameInfoImpl.N;
        this.gameInfoImpl = gameInfoImpl;
        this.playerName = name;

        //the left side of the GUI
        Panel players = new Panel();
        players.setLayout(new BoxLayout(players, BoxLayout.PAGE_AXIS));

        info = new JLabel[MAX_ROWS];
        for(int i = 0; i < MAX_ROWS; i++) {
            info[i] = new JLabel();
            info[i].setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            players.add(info[i]);
        }
        updateInfo();

        //the right side of the GUI
        Panel map = new Panel();
        map.setLayout(new GridLayout(rows, columns));
        this.map = new JLabel[rows][columns];
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {
                this.map[i][j] = new JLabel();
                this.map[i][j].setOpaque(true);
                this.map[i][j].setBorder(BorderFactory.createLineBorder(Color.black));
                map.add(this.map[i][j]);
            }
        }
        updateMap(rows, columns);

        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
        add(players, BorderLayout.WEST);
        setTitle(playerName);
        setSize(700, 500);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        gameInfoImpl = (GameInfoImpl) evt.getNewValue();
        updateInfo();
        updateMap(gameInfoImpl.N, gameInfoImpl.N);
    }

    /**
     * update the info of players
     */
    private void updateInfo(){
        if(gameInfoImpl.getMaster() != null) info[0].setText(" MASTER: " + gameInfoImpl.getMaster().name);
        if(gameInfoImpl.getSlave() != null) info[1].setText(" SLAVE: " + gameInfoImpl.getSlave().name);
        info[2].setText("--------------");
        int num = 2; //fixed rows
        for(Map.Entry<Player, PlayerInfo> entry : gameInfoImpl.getPlayersAndInfo().entrySet()){
            info[++num].setText(entry.getKey().name + " : " + entry.getValue().score);
        }
        while(num < MAX_ROWS - 1){
            info[++num].setText("");
        }
    }

    /**
     * update the map of maze
     * @param rows
     * @param columns
     */
    private void updateMap(int rows, int columns) {
        for(int i = 0;i < rows;i++){
            for(int j = 0;j < columns;j++){
                int pos = i * rows + j;
                String text = "";
                Color backgroundColor = Color.red;
                if(gameInfoImpl.getTreasures().contains(pos)){
                    backgroundColor = Color.yellow;
                    text = "#";
                }
                for(Map.Entry<Player, PlayerInfo> entry : gameInfoImpl.getPlayersAndInfo().entrySet()){
                    if(entry.getValue().position == pos){
                        text = entry.getKey().name;
                        backgroundColor = entry.getKey().name.equals(playerName) ? Color.green : Color.white;
                        break;
                    }
                }
                map[i][j].setText(text);
                map[i][j].setBackground(backgroundColor);
            }
        }
    }
}
