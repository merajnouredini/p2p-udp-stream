package ir.ac.iust.client;

import java.net.DatagramSocket;

/**
 * Stream Request Model Class
 *
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
