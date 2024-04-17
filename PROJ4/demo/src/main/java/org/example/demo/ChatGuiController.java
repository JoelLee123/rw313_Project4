package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * The ChatGuiController class is responsible for handling the user interface
 * interactions within the chat window of the chat application.
 */
public class ChatGuiController extends Application {
    private String username;
    private Client client;
    private VoIPClient voIPClient;
    @FXML
    private TextArea InputMessage;
    @FXML
    private TextArea TextAreaNames;
    @FXML
    private TextArea MessageOutput;
    @FXML
    private Button btnSendMessage, btnStartCall;

    /**
     * Default constructor for ChatGuiController.
     */
    public ChatGuiController() {
        // Default constructor is required for FXML loading
    }

    /**
     * The main entry point for the JavaFX application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Sets the username for the client using this chat GUI.
     *
     * @param username The username of the client.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the client instance associated with this chat GUI.
     *
     * @param client The client instance.
     */
    public void setClient(Client client) {
        this.client = client;
    }

    public void setVoipClient(VoIPClient voiIPClient) {
        this.voIPClient = voiIPClient;
    }

    /**
     * Handles the action of the "Send Message" button being clicked.
     * It sends the message to the server and displays it in the chat window.
     *
     * @param event The event that triggered the method call.
     */
    @FXML
    void btnSendMessageClicked(ActionEvent event) {
        String messageText = InputMessage.getText();
        Message message;

        if ("/leave".equalsIgnoreCase(messageText.trim())) {
            message = new Message("command", username, null, "/leave");
            this.displayMessage(message);

        } else if (messageText.startsWith("/w ")) {
            String[] parts = messageText.split(" ", 3);
            if (parts.length >= 3) {
                String recipient = parts[1];
                messageText = parts[2];
                message = new Message("private", username, recipient, messageText.trim());
                this.client.sendMessage(message);
                this.displayMessage(message);
            }

        } else if (!messageText.trim().isEmpty()) {
            message = new Message("broadcast", username, null, messageText.trim());
            this.client.sendMessage(message);
            this.displayMessage(message);
        }
        InputMessage.clear();
    }

    /**
     * Displays a message in the chat window.
     *
     * @param message The message to be displayed.
     */
    public void displayMessage(Message message) {
        Platform.runLater(() -> {
            String formattedMessage = formatMessage(message);
            MessageOutput.appendText(formattedMessage + "\n");
        });
    }

    /**
     * Starts the JavaFX application. This method is not used in this controller.
     *
     * @param primaryStage The primary stage for this application.
     * @throws Exception If an error occurs during application start.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    /**
     * Updates the list of active users displayed in the chat window.
     */
    @FXML
    public void updateUsers() {
        Platform.runLater(() -> {
            TextAreaNames.clear();
            TextAreaNames.appendText("USERS:  \n");
            for (String username : Server.activeUsernames) {
                TextAreaNames.appendText(username + "\n");
            }
        });
    }

    /**
     * Formats a message for display in the chat window.
     *
     * @param message The message to format.
     * @return A formatted string representation of the message.
     */
    private String formatMessage(Message message) {
        String formattedMessage = "";
        if (message.getType().equals("broadcast")) {
            if (message.getSender().equals(username)) {
                formattedMessage = "[You] " + message.getContent();
            } else {
                formattedMessage = "[" + message.getSender() + "] " + message.getContent();
            }
        } else if (message.getType().equals("private")) {
            if (message.getSender().equals(username)) {
                formattedMessage = "[You] (whisper to " + message.getRecipient() + ") " + message.getContent();
            } else {
                formattedMessage = "[" + message.getSender() + "] (whisper) " + message.getContent();
            }
        } else if (message.getType().equals("command") && message.getContent().equalsIgnoreCase("/leave")) {
            formattedMessage = "You have left the chat.";
            this.client.closeEverythingHelper();
            Platform.exit();
        }
        return formattedMessage;
    }
}
