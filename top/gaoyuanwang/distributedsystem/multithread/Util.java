package top.gaoyuanwang.distributedsystem.multithread;

import top.gaoyuanwang.distributedsystem.game.GameInfo;
import top.gaoyuanwang.distributedsystem.pojo.RequestType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The universal requests that will be used by all threads.
 */
public class Util {
    /**
     * Ping the player to check if the player is available.
     * @param port
     * @return
     */
    public static boolean ping(int port) {
        String ip = null;
        try(Socket socket = new Socket(ip, port)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(RequestType.REQUEST_PING);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            RequestType response = (RequestType) objectInputStream.readObject();
            return response.equals(RequestType.REQUEST_PONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * master assigns a slave.
     * @param masterPort
     * @param slavePort
     * @return boolean
     */
    public static boolean assignSlave(int masterPort, int slavePort) {
        String ip = null;
        System.out.println("slavePort: " + slavePort);
        try(Socket socket = new Socket(ip, slavePort)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            RequestType requestType = RequestType.REQUEST_ASSIGN_SLAVE;
            System.out.println("masterPort: " + masterPort);
            objectOutputStream.writeObject(requestType);
            objectOutputStream.writeObject(masterPort);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            RequestType response = (RequestType) objectInputStream.readObject();
            return response.equals(RequestType.REQUEST_SLAVE_ASSIGNED);
        } catch (Exception e) {
            System.err.println("Exception while sending REQUEST_ASSIGN_SLAVE msg to "+ slavePort + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Get game info from socket.
     * @param masterPort
     * @return GameInfo
     * @throws IOException
     */
    public static GameInfo getGameInfoFromSocket(int masterPort) throws IOException {
        String ip = null;
        GameInfo gameInfo = null;
        try(Socket socket = new Socket(ip, masterPort)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(RequestType.REQUEST_GAME_INFO);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            gameInfo = (GameInfo) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return gameInfo;
    }
}
