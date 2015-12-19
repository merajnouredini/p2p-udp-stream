package ir.ac.iust.server;

import ir.ac.iust.client.Client;
import ir.ac.iust.protocol.MessageProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by meraj on 12/19/15.
 */
public class Server {
    private static Map<String, Socket> clients = new HashMap<>();
    private static List<ClientHandler> handlers = new ArrayList<>();
    private static List<ClientUDP> streamChains = new ArrayList<>();
    private static ServerSocket serverSocket;
    public static ClientHandler streamRequester;
    private static int numOfStreamRequestAnswers = 0;


    public void startSending() {
        /*
        send data stream request and name to all clients
         */
    }

    public void makeConnectionChain() {
        /*
        manage sending order between clients using random distribution
         */
    }

    public void listen(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            handlers.add(handler);
            Thread t = new Thread(handler);
            t.start();
        }
    }

    public static synchronized void registerClient(String name, Socket socket) {
        clients.put(name, socket);
    }

    public static synchronized void unregisterClient(String clientName) {
        clients.remove(clientName);
    }

    public static synchronized void addToChain(ClientUDP clientUDP){
        streamChains.add(clientUDP);
    }

    public static synchronized void incrementNumberOfStreamRequestAnswers(){
        numOfStreamRequestAnswers ++;
    }

    public static synchronized void setCurrentRequester(ClientHandler handler){
        streamRequester = handler;
    }

    public static boolean isChainReady(){
        return numOfStreamRequestAnswers == handlers.size();
    }

    public static Socket[] getClientSockets() {
        return (Socket[]) clients.values().toArray();
    }

    public static ClientHandler[] getClientHandlers() {
        return (ClientHandler[]) handlers.toArray();
    }

    public static ArrayList<ClientUDP> getChain() {
        return (ArrayList<ClientUDP>) streamChains;
    }
}

/*
class UDPServer
{
   public static void main(String args[]) throws Exception
      {
         DatagramSocket serverSocket = new DatagramSocket(9876);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while(true)
               {
                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                  serverSocket.receive(receivePacket);
                  String sentence = new String( receivePacket.getData());
                  System.out.println("RECEIVED: " + sentence);
                  InetAddress IPAddress = receivePacket.getAddress();
                  int port = receivePacket.getPort();
                  String capitalizedSentence = sentence.toUpperCase();
                  sendData = capitalizedSentence.getBytes();
                  DatagramPacket sendPacket =
                  new DatagramPacket(sendData, sendData.length, IPAddress, port);
                  serverSocket.send(sendPacket);
               }
      }
} - See more at: https://systembash.com/a-simple-java-udp-server-and-udp-client/#sthash.5ZGOWq1r.dpuf
 */