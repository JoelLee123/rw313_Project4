package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The ChatGuiController class is responsible for handling the user interface
 * interactions within the chat window of the chat application.
 */
public class ChatGuiController extends Application {
    private String username;
    private Client client;

    @FXML
    private TextArea InputMessage;
    @FXML
    private TextArea TextAreaNames;
    @FXML
    private TextArea MessageOutput;
    @FXML
    private Button btnSendMessage, btnStartCall, btnVoicenote;

    // Voice Note attributes
    @FXML
    private VBox voiceNoteContainer;

    @FXML
    private TargetDataLine audioLine;
    private AudioFormat audioFormat;
    private ByteArrayOutputStream audioByteStream;
    private byte[] audioData;
    private final String BASE_ADDRESS = "ff02::1:";
    // Maybe I should set to tree when start recording
    // Then set back to false once recording has finished?
    private volatile boolean recording = true;

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

    public void setVoipClient(VoIPManager voiIPClient) {
        this.client.voIPClient = voiIPClient;
    }

    @FXML
    void btnSendMessageClicked(ActionEvent event) {
        String messageText = InputMessage.getText().trim(); // Ensure to trim any leading/trailing whitespace
        if (messageText.isEmpty()) {
            return; // Do not process empty messages
        }
        if (messageText.equals("/mute")) {
            this.client.voIPClient.muteMic();
        }

        if (messageText.equals("/unmute")) {
            this.client.voIPClient.unmuteMic();
        }

        if (messageText.startsWith("/")) {
            String[] parts = messageText.split(" ", 3); // Split only needed for commands
            if (parts[0].startsWith("/call") && parts.length == 2) {
                this.client.voIPClient.startCall(username);
                Message callMessage = new Message("call", username, parts[1], "call", false);
                this.client.sendMessage(callMessage);
            } else if (parts[0].startsWith("/deny") && parts.length == 2) {
                this.client.voIPClient.denyCall(username);
                Message callMessage = new Message("call", username, parts[1], "deny", false);
                this.client.sendMessage(callMessage);
            } else if (parts[0].startsWith("/leave") && parts.length == 2) {
                this.client.voIPClient.leaveCall(parts[1]);
            } else if (parts[0].startsWith("/accept") && parts.length == 2) {
                this.client.voIPClient.acceptCall(parts[1]);
                Message callMessage = new Message("call", username, parts[1], "accept", false);
                this.client.sendMessage(callMessage);
            } else if (parts[0].startsWith("/w") && parts.length == 3) {
                Message message = new Message("private", username, parts[1], parts[2].trim(), false);
                this.client.sendMessage(message);
                this.displayMessage(message);
            } else if (parts[0].equalsIgnoreCase("/exit")) {
                Message message = new Message("command", username, null, "/exit", false);
                this.displayMessage(message);
            } else {
                handleNormalMessage(messageText);
            }
        } else {
            handleNormalMessage(messageText);
        }
        InputMessage.clear();
    }

    /**
     * Handles normal (non-command) message sending and displaying.
     * 
     * @param messageText The text of the message to be sent.
     */
    private void handleNormalMessage(String messageText) {
        Message message = new Message("broadcast", username, null, messageText, false);
        this.client.sendMessage(message);
        this.displayMessage(message);
    }

    @FXML
    void btnStartCallClicked(ActionEvent event) {
        if (this.client.voIPClient == null) {
            System.out.println("VoIPClient has not been initialized.");
            return;
        }
        try {
            if (btnStartCall.getText().equals("Join VoiceChat")) {
                this.client.voIPClient.start(); // Start the VoIP call
                // Server.updateClientActivity(this.client.getUsername() + " clicked join
                // voicechat.");
                btnStartCall.setText("Leave VoiceChat");
            } else {
                this.client.voIPClient.leaveCall(null); // End the VoIP call
                // Server.updateClientActivity(this.client.getUsername() + " clicked leave
                // voicechat.");
                btnStartCall.setText("Join VoiceChat");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a message in the chat window.
     *
     * @param message The message to be displayed.
     */
    public void displayMessage(Message message) {
        Platform.runLater(() -> {
            if (!message.getIsAudio()) { // If not audio -> Must be text
                String formattedMessage = formatMessage(message);
                MessageOutput.appendText(formattedMessage + "\n");
            } else if (message.getIsAudio()) {
                displayVoiceNote(message);
            }
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
        } else if (message.getType().equals("call")) {
            if (message.getContent().equals("call")) {
                if (message.getSender().equals(username))
                    formattedMessage = "Attepmpting to call " + message.getRecipient();
                else
                    formattedMessage = message.getSender() + " is trying to call you";
            } else if (message.getContent().equals("deny")) {
                if (message.getSender().equals(username))
                    formattedMessage = "Denied call from " + message.getRecipient();
                else
                    formattedMessage = message.getRecipient() + " denied your call";
            } else if (message.getContent().equals("accept")) {
                if (message.getSender().equals(username))
                    formattedMessage = "Accepted call from " + message.getRecipient() + "... Say Hello!";
                else
                    formattedMessage = message.getRecipient() + " accepted your call";
            }
        } else if (message.getType().equals("command") && message.getContent().equalsIgnoreCase("/exit")) {
            formattedMessage = "You have left the chat.";
            this.client.closeEverythingHelper();
            Platform.exit();
        }
        return formattedMessage;
    }

    /*
     * =============================================================================
     * ===================================
     * VOICE NOTE METHODS
     * =============================================================================
     * ===================================
     */

    @FXML
    void btnVoicenoteClicked(ActionEvent event) {
        System.out.println("voicenote clicked");
        // Toggle the recording state based on the button text
        if (btnVoicenote.getText().equals("Record Voicenote")) {
            btnVoicenote.setText("Send Voicenote"); // Change button text to "Stop Recording"
            btnSendMessage.setDisable(true); // Optionally disable other UI elements while recording
            // Server.updateClientActivity(username + " recording voicenote.");
            recording = true; // Set the recording flag to true

            try {
                // Initialize or reinitialize the audio line
                audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Data line not supported");
                    return;
                }

                if (audioLine != null) {
                    audioLine.close();
                }
                audioLine = (TargetDataLine) AudioSystem.getLine(info);
                audioLine.open();
                audioLine.start();

                // Reset and prepare the byte stream for new recording data
                audioByteStream = new ByteArrayOutputStream();
                new Thread(this::recordAudio).start();
            } catch (LineUnavailableException e) {
                System.out.println("ERROR - Could not start recording");
                e.printStackTrace();
            }
        } else if (btnVoicenote.getText().equals("Send Voicenote")) {
            btnVoicenote.setText("Record Voicenote"); // Change button text back to "Start Recording"
            btnSendMessage.setDisable(false); // Re-enable other UI elements
            // Server.updateClientActivity(username + " sent voicenote.");

            // Stop the recording securely
            recording = false;
            if (audioLine != null) {
                audioLine.stop();
                audioLine.close();
            }

            // Process the recorded audio data
            audioData = audioByteStream.toByteArray();
            byte[] encodedAudioData = encodeAudioData(audioData);

            // Determine message type based on input prefix
            handleAudioMessage(InputMessage.getText().trim(), encodedAudioData);

            // Reset the stream for the next recording
            if (audioByteStream != null)
                audioByteStream.reset();
            InputMessage.clear(); // Clear input message text
        }
    }

    private void handleAudioMessage(String whisper, byte[] encodedAudioData) {
        Message audioMessage;
        if (whisper.startsWith("/w")) {
            String[] parts = whisper.split(" ", 3);
            if (parts.length == 2) {
                String recipient = parts[1];
                audioMessage = new Message("private", username, recipient, encodedAudioData, true);
            } else {
                return; // Exit method if the whisper command format is incorrect
            }
        } else {
            audioMessage = new Message("broadcast", username, null, encodedAudioData, true);
        }
        client.sendMessage(audioMessage);
        System.out.println("Message sent");
    }

    private void recordAudio() {
        byte[] buffer = new byte[4096];
        int bytesRead;
        try {
            while (recording && (bytesRead = audioLine.read(buffer, 0, buffer.length)) != -1) {
                if (bytesRead > 0) {
                    audioByteStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR - Could not read audio data");
            e.printStackTrace();
        }
    }

    private byte[] encodeAudioData(byte[] audioData) {
        try {
            // Encode the audioData to a suitable format (e.g., MP3)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), audioFormat, audioData.length / audioFormat.getFrameSize());
            // Decided to use WAV as audio quality is better - also easier to work with
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            System.out.println("ERROR - Could not encode audio data");
            e.printStackTrace();
            return null;
        }
    }

    void displayVoiceNote(Message message) {
        System.out.println("IN display voice Note");

        byte[] audioData = message.getAudioContent();
        String sender = message.getSender();
        // Unique filename for voice note based on time created
        String fileName = "voice_note_" + System.currentTimeMillis() + ".wav";

        VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName);

        System.out.println("Got here");

        Hyperlink voiceNoteLink = createVoiceNoteLink(audioData, sender, fileName);
        Platform.runLater(() -> voiceNoteContainer.getChildren().add(voiceNoteLink));

        System.out.println("Reachable");
    }

    Hyperlink createVoiceNoteLink(byte[] audioData, String sender, String fileName) {
        Hyperlink link = new Hyperlink("Voice note from " + sender);
        link.setOnAction(event -> {
            VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName);
            voiceNote.play();
        });
        return link;
    }
}
