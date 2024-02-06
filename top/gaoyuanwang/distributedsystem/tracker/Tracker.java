package top.gaoyuanwang.distributedsystem.tracker;

import java.net.Inet4Address;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Tracker {

    public static TrackerInfo trackerInfo;

    // IP address of the tracker
    public static String IPAddress;

    // Registry of the tracker
    public static Registry registry;

    /**
     * create a new remote object tracker
     * @param args
     */
    public static void main(String[] args) {
        //The default value of port is 1099, N is 15, K is 10
        Integer port = 1099;
        Integer N = 15;
        Integer K = 10;

        if(args.length != 0) {
            if (args.length < 3) {
                throw new IllegalArgumentException(
                        "Please input port number, N and K, or you can use the default value.");
            }
            port = Integer.valueOf(args[0]);
            N = Integer.valueOf(args[1]);
            K = Integer.valueOf(args[2]);
        }

        // Create a new trackerInfo
        TrackerInfoImpl trackerInfoImpl = new TrackerInfoImpl(N, K);

        // Get the IP address of the tracker
        try {
            IPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Export the trackerInfo to become a remote object
        try {
            trackerInfo = (TrackerInfo) UnicastRemoteObject.exportObject(trackerInfoImpl, 0);
            registry = LocateRegistry.getRegistry(port);
            registry.bind("Tracker", trackerInfo);
            System.out.println("Tracker ready at " + IPAddress + ":" + port);
        } catch (AlreadyBoundException e) {
            // If the tracker is already registered, rebind it
            try {
                System.err.println("Tracker is already on. Try to rebind ...");
                registry.unbind("Tracker");
                registry.bind("Tracker", trackerInfo);
                System.out.println("Tracker ready at " + IPAddress + ":" + port);
            } catch(Exception ex){
                ex.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
