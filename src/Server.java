import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

public class Server {

    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;
    private static final int maxClientsCount = 10;
    private static final int portNumber = 13425;

    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String[] args) {
        System.out.println("Server Started.");
        iport();

        try {
            Database database = new Database();
            GraphicalInterface gui = new GraphicalInterface();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            serverSocket = new ServerSocket(portNumber);
            int k = 0;
            while (true) {
                clientSocket = serverSocket.accept();
                for (k = 0; k < maxClientsCount; k++) {
                    if (threads[k] == null) {
                        (threads[k] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (k == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static void iport(){
        InetAddress ip;
        String hostname;
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
            System.out.println("Your current IP address : " + ip);
            System.out.println("Your current Hostname : " + hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
class clientThread extends Thread {
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private final int maxClientsCount;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }
    public void run(){
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());

            String name = is.readLine().trim(); // IMPORTANT
            for (int k = 0; k < maxClientsCount; k++) {
                if (threads[k] != null && threads[k] != this) {
                    threads[k].os.println( name + " has connected to the server.");
                }
            }
            while (true) {
                String line = is.readLine();
                System.out.println(line);
                if (line.startsWith("/exit")) {
                    break;
                }
                for (int k = 0; k < maxClientsCount; k++) {
                    if (threads[k] != null) {
                        threads[k].os.println("[" + name + "] : " + line);
                    }
                }
            }
            for (int k = 0; k < maxClientsCount; k++) {
                if (threads[k] != null && threads[k] != this) {
                    threads[k].os.println( name + " is disconnecting from the server.");
                }
            }
            for (int k = 0; k < maxClientsCount; k++) {
                if (threads[k] == this) {
                    threads[k] = null;
                }
            }
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}