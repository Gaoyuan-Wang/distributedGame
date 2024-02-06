package top.gaoyuanwang.distributedsystem.multithread;

import top.gaoyuanwang.distributedsystem.pojo.RequestType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

public class RequestHandlerThread extends Thread{

    private ListenerThread listenerThread;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Constructor for RequestHandlerThread
     * @param listenerThread
     * @param in
     * @param out
     */
    public RequestHandlerThread(ListenerThread listenerThread, ObjectInputStream in, ObjectOutputStream out){
        this.listenerThread = listenerThread;
        this.in = in;
        this.out = out;
    }

    /**
     * when a request is received, the ListenerThread will create a RequestHandlerThread to handle the request,
     * this is a traditional Blocking IO model
     */
    public void run(){
        try{
            while(true) {
                TimeUnit.MILLISECONDS.sleep(100);
                RequestType requestType = (RequestType) in.readObject();
                if (requestType == null) continue;
                switch (requestType) {
                    case REQUEST_GAME_INFO:
                        out.writeObject(listenerThread.gameInfo);
                        break;
                    case REQUEST_PING:
                        out.writeObject(RequestType.REQUEST_PONG);
                        break;
                }
                if(requestType == RequestType.REQUEST_ASSIGN_SLAVE){
                    int masterPort = (Integer) in.readObject();
                    listenerThread.tryRunSlaveThread(masterPort);
                    out.writeObject(RequestType.REQUEST_SLAVE_ASSIGNED);
                    break;
                }
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

}
