package ir.ac.iust.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.org.apache.xpath.internal.SourceTree;
import ir.ac.iust.protocol.MessageProtocol;
import ir.ac.iust.protocol.PKT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by meraj on 12/20/15.
 */
public class ServerCommunicator implements Runnable {
    private Client client;
    private Socket socket = null;
    private FileEvent fileEvent = null;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ServerCommunicator(Client client,Socket socket) throws IOException {
        this.socket = socket;
        this.client = client;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];
        try {
            int bytesread = 0;
            while (bytesread != -1) {
                // read packet header to obtain message size
                bytesread = inputStream.read(buffer, 0, 2);
                if(bytesread != -1) {
                    final int highByte = buffer[1] & 0xFF;
                    final int lowByte = buffer[0] & 0xFF;
                    final int messageSize = (lowByte) + (highByte << 8);
                    bytesread = inputStream.read(buffer, 2, messageSize);
                    byte[] data = new byte[messageSize-1];
                    System.arraycopy(buffer, 3, data, 0, messageSize - 1);
                    int type = buffer[2];
                    PKT pkt = new PKT(type, data);
                    processIncomingMessage(pkt);
                }
            }
            System.out.println(socket.getLocalPort() + " closed");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("socket closed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processIncomingMessage(PKT pkt) throws InterruptedException, InvalidProtocolBufferException {
        MessageProtocol.Response response;
        switch (pkt.type){
            case MessageProtocol.MessageType.REGISTER_RSP_VALUE:
                 response = MessageProtocol.Response.parseFrom(pkt.data);
                if(response.getStatus() == MessageProtocol.Status.SUCCESS) {
                    System.out.println("registered successfully");
                } else {
                    System.out.println(response.getMsg());
                }
                break;
            case MessageProtocol.MessageType.UNREGISTER_RSP_VALUE:
                response = MessageProtocol.Response.parseFrom(pkt.data);
                if(response.getStatus() == MessageProtocol.Status.SUCCESS) {
                    System.out.println("Unregistered");
                } else {
                    System.out.println(response.getMsg());
                }
                break;
            case MessageProtocol.MessageType.STREAM_REQUEST_VALUE:
                System.out.println("Stream Request received, you can choose option 4 to answer it.");
                MessageProtocol.StreamRequest request = MessageProtocol.StreamRequest.parseFrom(pkt.data);
                StreamRequest streamRequest = new StreamRequest(request.getStreamName());
                client.setStreamRequest(streamRequest);
                break;
            case MessageProtocol.MessageType.STREAM_REQUEST_RSP_VALUE:
                response = MessageProtocol.Response.parseFrom(pkt.data);
                if(response.getStatus() != MessageProtocol.Status.SUCCESS) {
                    System.out.println(response.getMsg());
                }
                break;
            case MessageProtocol.MessageType.STREAM_RSP_VALUE:
                try {
                    client.handleStreamResponse(pkt);
                } catch (InvalidProtocolBufferException e) {
                    System.out.println("invalid message received");
                }
                break;
        }
    }
}
