package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * The Client class handles the client-side functionality of the chat
 * application.
 * It manages the connection to the server, sending and receiving messages, and
 * updating the chat GUI.
 */
public class Client extends Application {
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;
    private ChatGuiController controller;
    public VoIPManager voIPClient;
    public Stage chatStage;

    /**
     * Constructs a Client instance with a specified socket, username, and
     * controller.
     *
     * @param socket     The socket connecting to the server.
     * @param username   The username of the client.
     * @param controller The controller for the chat GUI.
     */
    public Client(Socket socket, String username, ChatGuiController controller, VoIPManager voIPClient) {
        this.controller = controller;
        try {
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.username = username;
            this.voIPClient = voIPClient;
            // Send the username as a Message object to the server
            sendMessage(new Message("login", username, null, username));
            listenForMessage();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Default constructor for Client.
     */
    public Client() {
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Listens for messages from the server and handles them accordingly.
     */
    public void listenForMessage() {
        new Thread(() -> {
            Message messageFromServer;
            while (socket.isConnected()) {
                try {
                    messageFromServer = (Message) objectInputStream.readObject();
                    if (messageFromServer != null) {
                        if (messageFromServer.getContent().equals("Username is already taken.")) {
                            // Restart the client
                            Platform.runLater(() -> {
                                try {
                                    closeEverything(socket, objectInputStream, objectOutputStream);
                                    restartClient(username);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            stop();
                            return;
                        }
                        controller.displayMessage(messageFromServer);
                        controller.updateUsers();
                    } else {
                        closeEverything(socket, objectInputStream, objectOutputStream);
                        System.out.println("SERVER: Server down, disconnecting clients...");
                        System.exit(0);
                        break;
                    }
                } catch (Exception e) {
                    closeEverything(socket, objectInputStream, objectOutputStream);
                    break;
                }
            }
        }).start();
    }

    /**
     * Closes the socket and input/output streams.
     */
    public void closeEverythingHelper() {
        closeEverything(socket, objectInputStream, objectOutputStream);
    }

    /**
     * Closes the socket and input/output streams.
     *
     * @param socket             The socket to close.
     * @param objectInputStream  The input stream to close.
     * @param objectOutputStream The output stream to close.
     */
    public void closeEverything(Socket socket, ObjectInputStream objectInputStream,
            ObjectOutputStream objectOutputStream) {
        try {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The entry point for the JavaFX application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch();
    }

    /**
     * Starts the JavaFX application and loads the login screen.
     *
     * @param stage The primary stage for this application.
     * @throws IOException If the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("MainController.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Sets the chat stage for the client.
     *
     * @param chatStage The stage used for the chat window.
     */
    public void setChatStage(Stage chatStage) {
        this.chatStage = chatStage;
    }

    /**
     * Restarts the client when the username is already taken.
     * 
     * @param usernameTaken The username that is already taken.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void restartClient(String usernameTaken) throws IOException {
        Platform.runLater(() -> {
            if (chatStage != null) {
                chatStage.close();
            }
        });

        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("MainController.fxml"));
                Parent root = fxmlLoader.load();
                Stage stageRestart = new Stage();
                stageRestart.setTitle("Login");
                stageRestart.setScene(new Scene(root));
                stageRestart.show();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Username Taken");
                alert.setHeaderText(null);
                alert.setContentText(
                        "The username '" + usernameTaken + "' is already taken. Please choose a different username.");
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
