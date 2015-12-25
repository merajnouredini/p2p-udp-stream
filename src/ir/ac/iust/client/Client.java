package ir.ac.iust.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sun.org.apache.xpath.internal.SourceTree;
import ir.ac.iust.protocol.MessageProtocol;
import ir.ac.iust.protocol.PKT;
import org.omg.PortableServer.THREAD_POLICY_ID;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 * Created by meraj on 12/20/15.
 */
public class Client {

    private StreamRequest currentRequest;
    private Socket socket = null;
    private String clientName;
    public boolean isStreamRequestAvailable = false;
    private String hostName = "localHost";
    private String nextHost;
    private int nextPort;
    private String sourceFilePath = "/home/meraj/movie.mkv";
    private final int CHUNK_SIZE = 1024;

    public Client() {

    }

    public void setStreamRequest(StreamRequest request) {
        currentRequest = request;
    }

    /**
     * connects to server and runs ServerCommunicator instance in other thread
     * @param ip server ip
     * @param port server welcome port
     * @throws IOException
     */
    private void connectToServer(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        Thread thread = new Thread(new ServerCommunicator(this, socket));
        thread.start();
    }

    /**
     * creates a UDP socket, and waits for incoming stream in other thread,
     * if udpSenderSocket is available, streams the received packets to next client
     * @param listenPort port number, chosen by user
     * @throws SocketException
     */
    public void createAndListenSocket(final int listenPort) throws SocketException {
        currentRequest.udpReceiverSocket = new DatagramSocket(listenPort);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] incomingData = new byte[CHUNK_SIZE];
                    long time;
                    int i = 0;
                    while (true) {
                        time = System.nanoTime();
                        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                        currentRequest.udpReceiverSocket.receive(incomingPacket);
                        time = System.nanoTime() - time;
                        if (i % 10 == 0) {
                            System.out.println("download rate : " + incomingPacket.getLength() / (double) time + " Gbps");
                        }
                        byte[] data = incomingPacket.getData();
                        if (currentRequest.udpSenderSocket != null) {
                            stream(data);
                        }
                        i++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * used by first client in chain to start stream
     */
    public void stream() {
        try {
            InetAddress address = InetAddress.getByName(nextHost);
            File file = new File(sourceFilePath);
            BufferedInputStream inputStream =
                    new BufferedInputStream(new FileInputStream(file));
//            byte[] data = new byte[(int) file.length()];
            byte[] data = new byte[1024];
            long time;
            int bytesRead = 0, i = 0;
            while ((bytesRead = inputStream.read(data)) != -1) {
                time = System.nanoTime();
                DatagramPacket sendPacket =
                        new DatagramPacket(data, bytesRead, address, nextPort);
                currentRequest.udpSenderSocket.send(sendPacket);
                time = System.nanoTime() - time;
                if (i % 10 == 0) {
                    System.out.println("upload rate : " + sendPacket.getLength() / (float) time + "Gbps");
                }
                i++;
            }
            System.out.println("Stream finished");
            sendStreamResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * used by clients to send received packets to the next client in chain.
     * @param data
     */
    public void stream(byte[] data) {
        try {
            InetAddress address = InetAddress.getByName(nextHost);
            long time;
            time = System.nanoTime();
            DatagramPacket sendPacket =
                    new DatagramPacket(data, data.length, address, nextPort);
            currentRequest.udpSenderSocket.send(sendPacket);
            time = System.nanoTime() - time;
            System.out.println("upload rate : " + sendPacket.getLength() / (float) time + " Gbps");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * sends a register request message to server
     * @param clientName
     */
    private void registerInServer(String clientName) {
        MessageProtocol.RegisterClient.Builder builder = MessageProtocol.RegisterClient.newBuilder();
        builder.setType(MessageProtocol.MessageType.REGISTER);
        builder.setClientName(clientName);
        MessageProtocol.RegisterClient registerClient = builder.build();
        prepareAndSendMessage(registerClient, builder.getType());
    }

    /**
     * sends unregister request message to server
     */
    private void unregisterInServer() {
        MessageProtocol.UnregisterClient.Builder builder = MessageProtocol.UnregisterClient.newBuilder();
        builder.setType(MessageProtocol.MessageType.UNREGISTER);
        builder.setClientName(clientName);
        MessageProtocol.UnregisterClient unregisterClient = builder.build();
        prepareAndSendMessage(unregisterClient, builder.getType());
    }

    /**
     * sends stream request message to server
     * @param streamName
     */
    private void sendStreamRequest(String streamName) {
        MessageProtocol.StreamRequest.Builder builder = MessageProtocol.StreamRequest.newBuilder();
        builder.setType(MessageProtocol.MessageType.STREAM_REQUEST);
        builder.setStreamName(streamName);
        MessageProtocol.StreamRequest streamRequest = builder.build();
        prepareAndSendMessage(streamRequest, builder.getType());
        currentRequest = new StreamRequest(streamName);
    }

    /**
     * sends response to stream request message
     * @param isIntrested true if client wants to be in chain, otherwise, false.
     */
    public void sendStreamRequestResponse(boolean isIntrested) {
        MessageProtocol.Response.Builder builder = MessageProtocol.Response.newBuilder();
        builder.setType(MessageProtocol.MessageType.STREAM_REQUEST_RSP);
        if (isIntrested) {
            System.out.println("Enter receive port number : ");
            int i = -1;
            while (true) {
                String s = (new Scanner(System.in)).next();
                try {
                    i = Integer.parseInt(s);
                    if (i < 1024 || i > 6535) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Enter valid port Number!!");
                }
                break;
            }
            try {
                createAndListenSocket(i);
            } catch (SocketException e) {
                System.out.println("Cannot listen to this port");
            }
            builder.setStatus(MessageProtocol.Status.SUCCESS);
            builder.setMsg(currentRequest.udpReceiverSocket.getLocalAddress().getHostName() + ":" + i);
        } else {
            builder.setStatus(MessageProtocol.Status.FAILURE);
        }
        MessageProtocol.Response response = builder.build();
        prepareAndSendMessage(response, builder.getType());
    }

    /**
     * converts the Protobuf Message to byte[] and adds header to it.
     * @param msg
     * @param type
     */
    private void prepareAndSendMessage(Message msg, MessageProtocol.MessageType type) {
        byte[] pkt = new byte[msg.getSerializedSize() + 3];
        pkt[0] = (byte) ((msg.getSerializedSize() + 1) & 0xFF);
        pkt[1] = (byte) (((msg.getSerializedSize() + 1) >> 8) & 0xFF);
        pkt[2] = (byte) type.getNumber();
        byte[] temp = msg.toByteArray();
        for (int i = 3, j = 0; i < msg.getSerializedSize() + 3; i++, j++) {
            pkt[i] = temp[j];
        }
        sendMessage(pkt);
    }

    /**
     * sends response to stream message from server
     */
    private void sendStreamResponse() {
        MessageProtocol.Response.Builder builder = MessageProtocol.Response.newBuilder();
        builder.setType(MessageProtocol.MessageType.STREAM_RSP);
        builder.setStatus(MessageProtocol.Status.SUCCESS);
        MessageProtocol.Response streamRequest = builder.build();
        prepareAndSendMessage(streamRequest, builder.getType());
    }

    /**
     * sends a packet to server in other thread
     * @param pkt
     */
    public void sendMessage(final byte[] pkt) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        socket.getOutputStream().write(pkt);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * closes server connection socket
     * @throws IOException
     */
    public void closeConnection() throws IOException {
        socket.close();
    }

    /**
     * opens an udpSenderSocket to send received stream to the next client
     * @param pkt
     * @throws InvalidProtocolBufferException
     */
    public void handleStreamResponse(PKT pkt) throws InvalidProtocolBufferException {
        MessageProtocol.Response response = MessageProtocol.Response.parseFrom(pkt.data);
        if (response.getStatus() == MessageProtocol.Status.SUCCESS) {
            if ("start".equals(response.getMsg())) {
                if (nextHost == null || nextHost.equals("")) {
                    throw new InvalidStateException("no next host received");
                } else {
                    stream();
                }
            } else {
                String[] msg = response.getMsg().split(":");
                nextHost = msg[0];
                try {
                    nextPort = Integer.parseInt(msg[1]);
                    currentRequest.udpSenderSocket = new DatagramSocket();
                    System.out.println("I'll send to " + nextHost + ":" + nextPort);
                } catch (NumberFormatException e) {
                    System.out.println("bad response from server");
                } catch (SocketException e) {
                    System.out.println("unable to connect to host");
                }
            }
        }
    }

    /**
     * client command line program
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        Client client = new Client();
        try {
            client.connectToServer("127.0.0.1", 4444);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Choose an option:\n" +
                        "1. register in server\n" +
                        "2. unregister\n" +
                        "3. stream file\n" +
                        "4. answer to stream request\n" +
                        "0. exit"
        );
        Scanner input = new Scanner(System.in);
        String s = "";
        while (!s.equals("0")) {
            s = input.next();
            int option = 0;
            try {
                option = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                continue;
            }
            switch (option) {
                case 1:
                    System.out.println("Enter client name:");
                    client.clientName = input.next();
                    client.registerInServer(client.clientName);
                    break;
                case 2:
                    if (client.clientName == null || client.clientName.equals("")) {
                        System.out.println("Client is not registered yet !");
                        break;
                    }
                    client.unregisterInServer();
                    break;
                case 3:
                    System.out.println("Enter Stream Name : ");
                    s = input.next();
                    client.sendStreamRequest(s);
                    break;
                case 4:
                    if (client.currentRequest != null) {
                        System.out.println("do you want the" + client.currentRequest.name + " stream? (y,n) : ");
                        while (true) {
                            String i = input.next();
                            if (i.equals("y")) {
                                client.sendStreamRequestResponse(true);
                                break;
                            } else if (i.equals("n")) {
                                client.sendStreamRequestResponse(false);
                                break;
                            } else {
                                System.out.println("bad answer, answer with y or n :");
                            }
                        }
                        client.isStreamRequestAvailable = false;
                    } else {
                        System.out.println("No stream request received yet...");
                    }
                    break;
            }
        }
        try {
            client.closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
