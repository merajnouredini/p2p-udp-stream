package ir.ac.iust.server;


/**
 * Created by meraj on 12/19/15.
 */
public class ClientUDP {
    public String ip;
    public String port;
    public ClientHandler handler;

    public ClientUDP(String ip, String port, ClientHandler handler) {
        this.ip = ip;
        this.port = port;
        this.handler = handler;
    }
}
