package ir.ac.iust.server;

import java.io.Serializable;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by meraj on 12/19/15.
 */
public class ServerTemplates {

    public class FileEvent implements Serializable {

        public FileEvent() {
        }

        private static final long serialVersionUID = 1L;

        private String destinationDirectory;
        private String sourceDirectory;
        private String filename;
        private long fileSize;
        private byte[] fileData;
        private String status;

        public String getDestinationDirectory() {
            return destinationDirectory;
        }

        public void setDestinationDirectory(String destinationDirectory) {
            this.destinationDirectory = destinationDirectory;
        }

        public String getSourceDirectory() {
            return sourceDirectory;
        }

        public void setSourceDirectory(String sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public byte[] getFileData() {
            return fileData;
        }

        public void setFileData(byte[] fileData) {
            this.fileData = fileData;
        }
    }

    public class Server {
        private DatagramSocket socket = null;
        private FileEvent fileEvent = null;

        public Server() {

        }

        public void createAndListenSocket() {
            try {
                socket = new DatagramSocket(9876);
                byte[] incomingData = new byte[1024 * 1000 * 50];
                while (true) {
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    socket.receive(incomingPacket);
                    byte[] data = incomingPacket.getData();
                    ByteArrayInputStream in = new ByteArrayInputStream(data);
                    ObjectInputStream is = new ObjectInputStream(in);
                    fileEvent = (FileEvent) is.readObject();
                    if (fileEvent.getStatus().equalsIgnoreCase("Error")) {
                        System.out.println("Some issue happened while packing the data @ client side");
                        System.exit(0);
                    }
                    createAndWriteFile();   // writing the file to hard disk
                    InetAddress IPAddress = incomingPacket.getAddress();
                    int port = incomingPacket.getPort();
                    String reply = "Thank you for the message";
                    byte[] replyBytea = reply.getBytes();
                    DatagramPacket replyPacket =
                            new DatagramPacket(replyBytea, replyBytea.length, IPAddress, port);
                    socket.send(replyPacket);
                    Thread.sleep(3000);
                    System.exit(0);

                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
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

//        public static void main(String[] args) {
//            Server server = new Server();
//            server.createAndListenSocket();
//        }
    }


}