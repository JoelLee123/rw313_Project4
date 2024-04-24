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

public class Server extends Application {
    private ServerSocket serverSocket;
    public static Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    private static ServerController controller;
    public static Map<String, InetAddress> activeCalls = new ConcurrentHashMap<>();
    public static Set<InetAddress> availableAddresses = new HashSet<>();
    private static Server instance; // Singleton instance
    private static final String BASE_ADDRESS = "ff02::1:";
    static {
        try {
            for (int i = 2; i <= 10; i++) {
                availableAddresses.add(InetAddress.getByName(BASE_ADDRESS + i));
            }
        } catch (UnknownHostException e) {
            System.err.println("Failed to initialise available addresses: " + e.getMessage());
        }
    }

    public static synchronized Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    // Method to add a call to active calls
    public static void addActiveCall(String sender, InetAddress address) {
        activeCalls.put(sender, address);
    }

    // Method to remove a call from active calls
    public static InetAddress removeActiveCall(String sender) {
        return activeCalls.remove(sender);
    }

    // Method to get an active call by sender
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
                    serverSocket = new ServerSocket(4044);
                    startServer();
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

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ServerController getController() {
        return controller;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void updateActiveUserList() {
        StringBuilder sb = new StringBuilder();
        for (String username : activeUsernames) {
            sb.append(username).append("\n");
        }
        controller.updateActiveUsers(sb.toString());
    }

    public static void updateClientActivity(String activity) {
        controller.appendClientActivity(activity);
    }
}