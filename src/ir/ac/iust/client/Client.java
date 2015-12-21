package ir.ac.iust.client;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sun.org.apache.xpath.internal.SourceTree;
import ir.ac.iust.protocol.MessageProtocol;
import org.omg.PortableServer.THREAD_POLICY_ID;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Created by meraj on 12/19/15.
 */
public class Client {

    private DatagramSocket udpSocket = null;
    private Socket socket = null;
    private FileEvent fileEvent = null;
    private String clientName;

    public Client() {

    }

    private void connectToServer(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        Thread thread = new Thread(new ServerCommunicator(this,socket));
        thread.start();
    }

    public void createAndListenSocket(int listenPort) {
        try {
            udpSocket = new DatagramSocket(listenPort);
            byte[] incomingData = new byte[1024 * 1000 * 50];
            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                udpSocket.receive(incomingPacket);
                byte[] data = incomingPacket.getData();
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                ObjectInputStream is = new ObjectInputStream(in);
                fileEvent = (FileEvent) is.readObject();
                if (fileEvent.getStatus().equalsIgnoreCase("Error")) {
                    System.out.println("Some issue happened while packing the data @ client side");
//                    System.exit(0);
                }
                createAndWriteFile();   // writing the file to hard disk
//                InetAddress IPAddress = incomingPacket.getAddress();
//                int port = incomingPacket.getPort();
//                String reply = "Thank you for the message";
//                byte[] replyBytea = reply.getBytes();
//                DatagramPacket replyPacket =
//                        new DatagramPacket(replyBytea, replyBytea.length, IPAddress, port);
//                udpSocket.send(replyPacket);
//                Thread.sleep(3000);
//                System.exit(0);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void createAndWriteFile() {
        String outputFile = fileEvent.getDestinationDirectory() + fileEvent.getFilename();
        if (!new File(fileEvent.getDestinationDirectory()).exists()) {
            new File(fileEvent.getDestinationDirectory()).mkdirs();
        }
        File dstFile = new File(outputFile);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(dstFile);
            fileOutputStream.write(fileEvent.getFileData());
            fileOutputStream.flush();
            fileOutputStream.close();
            System.out.println("Output file : " + outputFile + " is successfully saved ");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void acknowledgeDownload() {
        /*
        open a udp socket and send ip and port to server
         */
    }

    private void registerInServer(String clientName) {
        MessageProtocol.RegisterClient.Builder builder = MessageProtocol.RegisterClient.newBuilder();
        builder.setType(MessageProtocol.MessageType.REGISTER);
        builder.setClientName(clientName);
        MessageProtocol.RegisterClient registerClient = builder.build();
        prepareAndSendMessage(registerClient, builder.getType());
    }

    private void unregisterInServer() {
        MessageProtocol.UnregisterClient.Builder builder = MessageProtocol.UnregisterClient.newBuilder();
        builder.setType(MessageProtocol.MessageType.UNREGISTER);
        builder.setClientName(clientName);
        MessageProtocol.UnregisterClient unregisterClient = builder.build();
        prepareAndSendMessage(unregisterClient, builder.getType());
    }

    private void sendStreamRequest(String streamName) {
        MessageProtocol.StreamRequest.Builder builder = MessageProtocol.StreamRequest.newBuilder();
        builder.setType(MessageProtocol.MessageType.STREAM_REQUEST);
        builder.setStreamName(streamName);
        MessageProtocol.StreamRequest streamRequest = builder.build();
        prepareAndSendMessage(streamRequest, builder.getType());
    }

    public void sendStreamRequestResponse(boolean isIntrested) {
        MessageProtocol.Response.Builder builder = MessageProtocol.Response.newBuilder();
        builder.setType(MessageProtocol.MessageType.STREAM_REQUEST_RSP);
        if(isIntrested) {
            System.out.println("Enter receive port number : ");
            int i = -1;
            while (true) {
                String s = (new Scanner(System.in)).next();
                try {
                    i = Integer.parseInt(s);
                    if( i < 1024 || i > 6535){
                        throw new NumberFormatException();
                    }
                }catch (NumberFormatException e){
                    System.out.println("Enter valid port Number!!");
                }
                break;
            }
            builder.setStatus(MessageProtocol.Status.SUCCESS);
            builder.setMsg(socket.getInetAddress().getHostName()+":"+i);
        } else {

        }
        MessageProtocol.Response response = builder.build();
        prepareAndSendMessage(response, builder.getType());
    }

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

    public void closeConnection() throws IOException {
        socket.close();
    }

    public static void main(String[] args) {
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
                        "0. exit"
        );
        Scanner input = new Scanner(System.in);
        String s = "";
        while (!s.equals("0")) {
            s = input.next();
            int option = Integer.parseInt(s);
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
                    s = input.next();
                    client.sendStreamRequest(s);
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
