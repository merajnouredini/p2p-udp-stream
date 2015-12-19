package ir.ac.iust.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.istack.internal.Nullable;
import ir.ac.iust.client.Client;
import ir.ac.iust.protocol.MessageProtocol;
import sun.plugin2.message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by meraj on 12/19/15.
 */
public class ClientHandler implements Runnable{
    private String clientName;
    private Socket socket;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        System.out.println(socket.getLocalPort() + " connected");
        byte[] buffer = new byte[4096];
        try {
//            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int bytesread = 0;
            while (bytesread != -1) {
                // read packet header to obtain message size
                bytesread = inputStream.read(buffer, 0, 2);
                if(bytesread != -1) {
                    final int highByte = buffer[1] & 0xFF;
                    final int lowByte = buffer[0] & 0xFF;
                    final int messageSize = (lowByte) + (highByte << 8);
//                    String msg = new String(buffer,0,bytesread);
//                    processIncomingMessage(msg);
                    bytesread = inputStream.read(buffer, 2, messageSize);
                    byte[] data = new byte[messageSize-1];
                    int type = buffer[0];
                    System.arraycopy(buffer, 1, data, 1, messageSize - 1);
                    PKT pkt = new PKT(type, data);
                    processIncomingMessage(pkt);
                }
            }
            System.out.println(socket.getLocalPort() + " closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processIncomingMessage(PKT pkt) throws InvalidProtocolBufferException {
        switch (pkt.type){
            case MessageProtocol.MessageType.REGISTER_VALUE:
                registerClient(pkt);
                break;
            case MessageProtocol.MessageType.UNREGISTER_VALUE:
                unregisterClient(pkt);
                break;
            case MessageProtocol.MessageType.STREAM_REQUEST_VALUE:
                handleStreamRequest(pkt);
                break;
            case MessageProtocol.MessageType.STREAM_REQUEST_RSP_VALUE:

                break;
//            case MessageProtocol.MessageType.STREAM_VALUE:
//                break;
        }
    }

    private void registerClient(PKT pkt) throws InvalidProtocolBufferException {
        MessageProtocol.RegisterClient registerClient = MessageProtocol.RegisterClient.parseFrom(pkt.data);
        this.clientName = registerClient.getClientName();
        Server.registerClient(registerClient.getClientName(), socket);
        sendResponse(MessageProtocol.MessageType.REGISTER_RSP, MessageProtocol.Status.SUCCESS, null);
    }

    private void unregisterClient(PKT pkt) throws InvalidProtocolBufferException {
        MessageProtocol.UnregisterClient unregisterClient = MessageProtocol.UnregisterClient.parseFrom(pkt.data);
        Server.unregisterClient(unregisterClient.getClientName());
        sendResponse(MessageProtocol.MessageType.UNREGISTER_RSP, MessageProtocol.Status.SUCCESS, null);
    }

    private void handleStreamRequest(PKT pkt) throws InvalidProtocolBufferException {
        if(Server.streamRequester == null) {
            MessageProtocol.StreamRequest streamRequest = MessageProtocol.StreamRequest.parseFrom(pkt.data);
            byte[] p = new byte[streamRequest.getSerializedSize() + 3];
            p[0] = (byte) ((streamRequest.getSerializedSize() + 1) & 0xFF);
            p[1] = (byte) (((streamRequest.getSerializedSize() + 1) >> 8) & 0xFF);
            p[2] = (byte) streamRequest.getType().getNumber();
            byte[] temp = streamRequest.toByteArray();
            for (int i = 3, j = 0; i < streamRequest.getSerializedSize() + 3; i++, j++) {
                p[i] = temp[j];
            }

            ClientHandler[] handlers = Server.getClientHandlers();
            for (ClientHandler handler : handlers) {
                handler.sendMessage(p);
            }
        } else {
            sendResponse(MessageProtocol.MessageType.STREAM_REQUEST_RSP,
                    MessageProtocol.Status.FAILURE, "pending chain error");
        }
    }

    private void handleStreamRequestResponse(PKT pkt) throws InvalidProtocolBufferException {
        MessageProtocol.Response response = MessageProtocol.Response.parseFrom(pkt.data);
        if(response.getStatus() == MessageProtocol.Status.SUCCESS) {
            String[] message = response.getMsg().split(":");
            String ip = message[0];
            String port = message[1];
            ClientUDP udp = new ClientUDP(ip, port, this);
            Server.addToChain(udp);
        }
        Server.incrementNumberOfStreamRequestAnswers();
        if (Server.isChainReady()){
            Server.streamRequester.sendResponse(MessageProtocol.MessageType.STREAM_RSP, MessageProtocol.Status.SUCCESS,
                    "start");
        }
    }

    private void sendResponse(MessageProtocol.MessageType type,
                              MessageProtocol.Status status,
                              @Nullable String message) {
        MessageProtocol.Response.Builder builder = MessageProtocol.Response.newBuilder();
        builder.setMsg(message);
        builder.setStatus(status);
        builder.setType(type);
        MessageProtocol.Response response = builder.build();
        byte[] pkt = new byte[response.getSerializedSize() + 3];
        pkt[0] = (byte) ((response.getSerializedSize() + 1) & 0xFF) ;
        pkt[1] = (byte) (((response.getSerializedSize() + 1) >> 8) & 0xFF) ;
        pkt[2] = (byte) type.getNumber();
        byte[] temp = response.toByteArray();
        for(int i=3,  j=0; i< response.getSerializedSize()+3; i++, j++){
            pkt[i] = temp[j];
        }
        sendMessage(pkt);
    }

    public void sendMessage(final byte[] pkt){
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        outputStream.write(pkt);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private class PKT{
        public int type;
        public byte[] data;

        public PKT(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
}
