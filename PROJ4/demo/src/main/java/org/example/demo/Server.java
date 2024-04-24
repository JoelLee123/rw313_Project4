package org.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.UnknownHostException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashSet;

/**
 * The Server class handles the main server operations for a chat application.
 * It manages user connections, voice over IP (VoIP) functionalities, and tracks
 * active users and calls.
 */
public class Server extends Application {
    private ServerSocket serverSocket; // Socket that listens for incoming client connections
    // Set to hold active usernames to ensure uniqueness and manage user sessions
    public static Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    private static ServerController controller; // Controller for managing server UI updates
    // Map to store active VoIP calls and their associated addresses
    public static Map<String, InetAddress> activeCalls = new ConcurrentHashMap<>();
    // Set to hold available addresses for VoIP calls
    public static Set<InetAddress> availableAddresses = new HashSet<>();
    private static Server instance; // Singleton instance of the Server
    private static final String BASE_ADDRESS = "ff02::1:"; // Base address for multicast VoIP calls

    static {
        try {
            // Initialize the availableAddresses set with a range of addresses for multicast
            for (int i = 2; i <= 10; i++) {
                availableAddresses.add(InetAddress.getByName(BASE_ADDRESS + i));
            }
        } catch (UnknownHostException e) {
            System.err.println("Failed to initialise available addresses: " + e.getMessage());
        }
    }

    /**
     * Get the singleton instance of the Server. If it doesn't exist, it creates
     * one.
     * 
     * @return the singleton instance of Server
     */
    public static synchronized Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    /**
     * Adds an active VoIP call to the map of active calls.
     * 
     * @param sender  Username of the sender initiating the call
     * @param address IP address assigned to the call
     */
    public static void addActiveCall(String sender, InetAddress address) {
        activeCalls.put(sender, address);
    }

    /**
     * Removes an active VoIP call from the map based on the sender's username.
     * 
     * @param sender Username of the sender whose call is to be removed
     * @return The IP address that was associated with the removed call
     */
    public static InetAddress removeActiveCall(String sender) {
        return activeCalls.remove(sender);
    }

    /**
     * Retrieves the IP address associated with an active VoIP call given a sender's
     * username.
     * 
     * @param sender Username of the sender
     * @return IP address associated with the sender's active call
     */
    public static InetAddress getActiveCall(String sender) {
        return activeCalls.get(sender);
    }

    @Override
    public void start(Stage primaryStage) {
        if (instance == null) {
            instance = this; // Initialize the singleton instance
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerGUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            primaryStage.setTitle("Server");
            primaryStage.setScene(scene);
            primaryStage.show();

            controller = fxmlLoader.getController();

            Thread serverThread = new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(4044); // Create a server socket on port 4044
                    startServer(); // Start the server
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the server to accept incoming client connections and manage client
     * threads.
     */
    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // Accept incoming connection
                ClientHandler clientHandler = new ClientHandler(socket); // Create a new ClientHandler for the
                                                                         // connection
                Thread thread = new Thread(clientHandler); // Create a new thread for the ClientHandler
                thread.start(); // Start the thread
            }
        } catch (IOException e) {
            closeServerSocket(); // Close the server socket if an exception occurs
        }
    }

    /**
     * Closes the server socket to stop accepting any new connections.
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
     * Retrieves the ServerController instance used for UI updates.
     * 
     * @return the instance of ServerController
     */
    public static ServerController getController() {
        return controller;
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }

    /**
     * Updates the active user list displayed on the server GUI.
     */
    public static void updateActiveUserList() {
        StringBuilder sb = new StringBuilder();
        for (String username : activeUsernames) {
            sb.append(username).append("\\n");
        }
        controller.updateActiveUsers(sb.toString());
    }

    /**
     * Appends a log of client activity to the server GUI to assist in monitoring.
     * 
     * @param activity Description of the client activity
     */
    public static void updateClientActivity(String activity) {
        controller.appendClientActivity(activity);
    }
}
