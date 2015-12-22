package ir.ac.iust.client;

import java.net.DatagramSocket;

/**
 * Created by meraj on 12/22/15.
 */
public class StreamRequest {
    public String name;
    public DatagramSocket udpReceiverSocket = null;
    public DatagramSocket udpSenderSocket = null;

    public StreamRequest(String name) {
        this.name = name;
    }
}
