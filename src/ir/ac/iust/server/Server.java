package ir.ac.iust.server;

import ir.ac.iust.client.Client;
import ir.ac.iust.protocol.MessageProtocol;

import javax.management.openmbean.KeyAlreadyExistsException;
import javax.swing.event.CaretListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

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

    /**
     * opens a ServerSocket and waits for clients to connect,
     * runs a ClientHandler instance in other thread for connected clients
     * @param port
     * @throws IOException
     */
    public static void listen(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Waiting for clients on port " + port + " ...");
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected...");
            ClientHandler handler = new ClientHandler(socket);
            Thread t = new Thread(handler);
            t.start();
        }
    }

    /**
     * register a client in server
     * @param name client name
     * @param socket client connection socket
     * @throws KeyAlreadyExistsException
     */
    public static synchronized void registerClient(String name, Socket socket) throws KeyAlreadyExistsException {
        if(!clients.containsKey(name)) {
            clients.put(name, socket);
        } else {
            throw new KeyAlreadyExistsException();
        }
    }

    /**
     * unregister client from server
     * @param clientName
     * @throws NoSuchElementException
     */
    public static synchronized void unregisterClient(String clientName) throws NoSuchElementException {
        if(clients.containsKey(clientName)) {
            clients.remove(clientName);
            if(Server.streamRequester.getClientName().equals(clientName)){
                Server.streamRequester = null;
                Server.emptyChain();
                Server.resetNumberOfStreamRequestAnswers();
            }
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * checks if every clients respond to Stream Request or not
     * @return ture if ready
     */
    public static boolean isChainReady(){
        return numOfStreamRequestAnswers == handlers.size()-1;
    }

    public static synchronized void addHandler(ClientHandler handler){
        handlers.add(handler);
    }

    public static synchronized void addToChain(ClientUDP clientUDP){
        streamChains.add(clientUDP);
    }

    public static synchronized void incrementNumberOfStreamRequestAnswers(){
        numOfStreamRequestAnswers ++;
    }

    public static synchronized void resetNumberOfStreamRequestAnswers(){
        numOfStreamRequestAnswers = 0;
    }

    public static List<ClientHandler> getClientHandlers() {
        return handlers;
    }

    public static ArrayList<ClientUDP> getChain() {
        return (ArrayList<ClientUDP>) streamChains;
    }

    public static void emptyChain() {
        streamChains.clear();
    }

    /**
     * runs a server on port 4444
     * @param args
     */
    public static void main(String[] args) {
        try {
            Server.listen(4444);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}