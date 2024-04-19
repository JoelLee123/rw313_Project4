package org.example.demo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Server class is responsible for handling the server-side operations
 * of the chat application. It listens for incoming client connections and
 * creates a new thread for each connected client.
 */
public class Server {

    private final ServerSocket serverSocket;
    public static Set<String> activeUsernames = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a Server instance with a specified ServerSocket.
     *
     * @param serverSocket The ServerSocket to be used for listening to incoming
     *                     connections.
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Starts the server and listens for incoming client connections.
     * When a new client connects, it starts a new ClientHandler thread to handle
     * the client.
     */
    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    /**
     * Closes the server socket and releases any system resources associated with
     * it.
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main entry point for the server application.
     *
     * @param args Command-line arguments (not used).
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(4044);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
