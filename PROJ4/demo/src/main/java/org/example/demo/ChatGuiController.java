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
 * It manages sending and receiving text messages, voice notes, and VoIP calls.
 */
public class ChatGuiController extends Application {
    private String username; // The username of the current client
    private Client client; // The client instance associated with this GUI

    @FXML
    private TextArea InputMessage; // TextArea for user input messages
    @FXML
    private TextArea TextAreaNames; // TextArea for displaying active users
    @FXML
    private TextArea MessageOutput; // TextArea for displaying chat messages
    @FXML
    private Button btnSendMessage, btnStartCall, btnVoicenote; // Buttons for sending messages, starting/joining calls,
                                                               // and recording voice notes

    // Voice Note attributes
    @FXML
    private VBox voiceNoteContainer; // Container for displaying voice note links

    @FXML
    private TargetDataLine audioLine; // Audio line for recording voice notes
    private AudioFormat audioFormat; // Audio format for recording
    private ByteArrayOutputStream audioByteStream; // Stream for storing recorded audio data
    private byte[] audioData; // Byte array for storing encoded audio data
    private final String BASE_ADDRESS = "ff02::1:"; // Base address for VoIP calls (not used)
    // Flag to control the recording state
    private volatile boolean recording = true; // Flag to indicate if recording is in progress

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

    /**
     * Sets the VoIP client instance associated with this chat GUI.
     *
     * @param voiIPClient The VoIP client instance.
     */
    public void setVoipClient(VoIPManager voiIPClient) {
        this.client.voIPClient = voiIPClient;
    }

    /**
     * Handles the send message button click event.
     * Processes the message and sends it based on the content.
     *
     * @param event The action event triggered by the button click.
     */
    @FXML
    void btnSendMessageClicked(ActionEvent event) {
        String messageText = InputMessage.getText().trim(); // Ensure to trim any leading/trailing whitespace
        if (messageText.isEmpty()) {
            return; // Do not process empty messages
        }

        // Handle mute/unmute commands
        if (messageText.equals("/mute")) {
            this.client.voIPClient.muteMic();
        } else if (messageText.equals("/unmute")) {
            this.client.voIPClient.unmuteMic();
        } else if (messageText.startsWith("/")) { // If the message starts with a '/', it is a command
            String[] parts = messageText.split(" ", 3); // Split the message into command and arguments

            // Handle VoIP call commands
            if (parts[0].startsWith("/call") && parts.length == 2) { // /call <recipient>
                this.client.voIPClient.startCall(username);
                Message callMessage = new Message("call", username, parts[1], "call", false);
                this.client.sendMessage(callMessage);
                this.displayMessage(callMessage);
            } else if (parts[0].startsWith("/deny") && parts.length == 2) { // /deny <sender>
                this.client.voIPClient.denyCall(username);
                Message callMessage = new Message("call", username, parts[1], "deny", false);
                this.client.sendMessage(callMessage);
                this.displayMessage(callMessage);
            } else if (parts[0].startsWith("/leave") && parts.length == 2) { // /leave <recipient>
                this.client.voIPClient.leaveCall(parts[1]);
                Message callMessage = new Message("call", username, parts[1], "leave", false);
                this.client.sendMessage(callMessage);
                this.displayMessage(callMessage);
            } else if (parts[0].startsWith("/accept") && parts.length == 2) { // /accept <sender>
                this.client.voIPClient.acceptCall(parts[1]);
                Message callMessage = new Message("call", username, parts[1], "accept", false);
                this.client.sendMessage(callMessage);
                this.displayMessage(callMessage);
            } else if (parts[0].startsWith("/w") && parts.length == 3) { // /w <recipient> <message>
                Message message = new Message("private", username, parts[1], parts[2].trim(), false);
                this.client.sendMessage(message);
                this.displayMessage(message);
            } else if (parts[0].equalsIgnoreCase("/exit")) { // /exit command
                Message message = new Message("command", username, null, "/exit", false);
                this.displayMessage(message);
            } else {
                handleNormalMessage(messageText); // Handle any other message as a normal message
            }
        } else {
            handleNormalMessage(messageText); // If the message does not start with '/', handle it as a normal message
        }
        InputMessage.clear(); // Clear the input message text area
    }

    /**
     * Handles normal (non-command) message sending and displaying.
     * 
     * @param messageText The text of the message to be sent.
     */
    private void handleNormalMessage(String messageText) {
        Message message = new Message("broadcast", username, null, messageText, false); // Create a broadcast message
        this.client.sendMessage(message); // Send the message to the server
        this.displayMessage(message); // Display the message in the chat window
    }

    /**
     * Handles the start/join VoIP call button click event.
     * Starts or leaves the VoIP call based on the current state.
     *
     * @param event The action event triggered by the button click.
     */
    @FXML
    void btnStartCallClicked(ActionEvent event) {
        if (this.client.voIPClient == null) {
            System.out.println("VoIPClient has not been initialized.");
            return;
        }
        try {
            if (btnStartCall.getText().equals("Join VoiceChat")) { // If the button text is "Join VoiceChat"
                this.client.voIPClient.start(); // Start the VoIP call
                // Server.updateClientActivity(this.client.getUsername() + " clicked join
                // voicechat.");
                btnStartCall.setText("Leave VoiceChat"); // Change the button text to "Leave VoiceChat"
            } else { // If the button text is "Leave VoiceChat"
                this.client.voIPClient.leaveCall(null); // End the VoIP call
                // Server.updateClientActivity(this.client.getUsername() + " clicked leave
                // voicechat.");
                btnStartCall.setText("Join VoiceChat"); // Change the button text to "Join VoiceChat"
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * Displays a message in the chat window.
     * Handles text and audio messages differently.
     *
     * @param message The message to be displayed.
     */
    public void displayMessage(Message message) {
        Platform.runLater(() -> {
            if (!message.getIsAudio()) { // If the message is not audio (text message)
                String formattedMessage = formatMessage(message); // Format the message for display
                MessageOutput.appendText(formattedMessage + "\n");
            } else if (message.getIsAudio()) { // If the message is audio (voice note)
                displayVoiceNote(message); // Display the voice note in the chat window
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
            TextAreaNames.clear(); // Clear the TextAreaNames
            TextAreaNames.appendText("USERS:  \n");
            for (String username : Server.activeUsernames) { // Iterate over the list of active usernames
                TextAreaNames.appendText(username + "\n"); // Append each username to the TextAreaNames
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
        if (message.getType().equals("broadcast")) { // If the message is a broadcast message
            if (message.getSender().equals(username)) { // If the sender is the current user
                formattedMessage = "[You] " + message.getContent(); // Format as "[You] <message content>"
            } else {
                formattedMessage = "[" + message.getSender() + "] " + message.getContent();
            }
        } else if (message.getType().equals("private")) { // If the message is a private message
            if (message.getSender().equals(username)) { // If the sender is the current user
                formattedMessage = "[You] (whisper to " + message.getRecipient() + ") " + message.getContent();
            } else {
                formattedMessage = "[" + message.getSender() + "] (whisper) " + message.getContent();
            }
        } else if (message.getType().equals("call")) { // If the message is a call message
            if (message.getContent().equals("call")) { // If the call message is for initiating a call
                if (message.getSender().equals(username))
                    formattedMessage = "Attempting to call " + message.getRecipient();
                else
                    formattedMessage = message.getSender() + " is trying to call you";
            } else if (message.getContent().equals("deny")) { // If the call message is for denying a call
                if (message.getSender().equals(username))
                    formattedMessage = "Denied call from " + message.getRecipient();
                else
                    formattedMessage = message.getRecipient() + " denied your call";
            } else if (message.getContent().equals("accept")) { // If the call message is for accepting a call
                if (message.getSender().equals(username))
                    formattedMessage = "Accepted call from " + message.getRecipient() + "... Say Hello!";
                else
                    formattedMessage = message.getRecipient() + " accepted your call";
            } else if (message.getContent().equals("leave")) { // If the call message is for leaving a call
                if (message.getSender().equals(username))
                    formattedMessage = "Left call with " + message.getRecipient();
                else
                    formattedMessage = message.getRecipient() + " left the call";
            }
        } else if (message.getType().equals("command") && message.getContent().equalsIgnoreCase("/exit")) {
            formattedMessage = "You have left the chat."; // Format as "You have left the chat."
            this.client.closeEverythingHelper(); // Close the client
            Platform.exit(); // Exit the JavaFX application
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

    /**
     * Handles the voice note button click event.
     * Starts or stops recording a voice note based on the current state.
     *
     * @param event The action event triggered by the button click.
     */
    @FXML
    void btnVoicenoteClicked(ActionEvent event) {
        // Toggle the recording state based on the button text
        if (btnVoicenote.getText().equals("Record Voicenote")) {
            btnVoicenote.setText("Send Voicenote"); // Change button text to "Send Voicenote"
            btnSendMessage.setDisable(true); // Disable the send message button while recording
            // Server.updateClientActivity(username + " recording voicenote.");
            recording = true; // Set the recording flag to true

            try {
                // Initialize or reinitialize the audio line
                audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat); // Get the data line info
                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Data line not supported");
                    return;
                }

                if (audioLine != null) {
                    audioLine.close(); // Close the existing audio line if it exists
                }
                audioLine = (TargetDataLine) AudioSystem.getLine(info); // Get a new audio line
                audioLine.open(); // Open the audio line
                audioLine.start(); // Start the audio line

                // Reset and prepare the byte stream for new recording data
                audioByteStream = new ByteArrayOutputStream(); // Create a new byte stream
                new Thread(this::recordAudio).start(); // Start a new thread for recording audio
            } catch (LineUnavailableException e) {
                System.out.println("ERROR - Could not start recording");
                e.printStackTrace();
            }
        } else if (btnVoicenote.getText().equals("Send Voicenote")) {
            btnVoicenote.setText("Record Voicenote"); // Change button text back to "Record Voicenote"
            btnSendMessage.setDisable(false); // Re-enable the send message button

            // Server.updateClientActivity(username + " sent voicenote.");

            // Stop the recording securely
            recording = false; // Set the recording flag to false
            if (audioLine != null) {
                audioLine.stop(); // Stop the audio line
                audioLine.close(); // Close the audio line
            }

            // Process the recorded audio data
            audioData = audioByteStream.toByteArray(); // Get the recorded audio data asa byte array
            byte[] encodedAudioData = encodeAudioData(audioData); // Encode the audio data

            // Determine message type based on input prefix
            handleAudioMessage(InputMessage.getText().trim(), encodedAudioData); // Handle the audio message

            // Reset the stream for the next recording
            if (audioByteStream != null)
                audioByteStream.reset(); // Reset the byte stream
            InputMessage.clear(); // Clear the input message text
        }
    }

    /**
     * Handles the audio message based on the input text.
     * Creates a broadcast or private message with the encoded audio data.
     *
     * @param whisper          The input text (whisper command or normal message).
     * @param encodedAudioData The encoded audio data.
     */
    private void handleAudioMessage(String whisper, byte[] encodedAudioData) {
        Message audioMessage;
        if (whisper.startsWith("/w")) { // If the input starts with "/w", it's a private message
            String[] parts = whisper.split(" ", 3); // Split the input into command, recipient, and content
            if (parts.length == 2) {
                String recipient = parts[1];
                audioMessage = new Message("private", username, recipient, encodedAudioData, true); // private message
            } else {
                return; // Exit method if the whisper command format is incorrect
            }
        } else {
            audioMessage = new Message("broadcast", username, null, encodedAudioData, true); // broadcast message
        }
        client.sendMessage(audioMessage); // Send the audio message
        System.out.println("Message sent");
    }

    /**
     * Records audio data from the audio line.
     */
    private void recordAudio() {
        byte[] buffer = new byte[4096]; // Buffer for reading audio data
        int bytesRead;
        try {
            while (recording && (bytesRead = audioLine.read(buffer, 0, buffer.length)) != -1) { // Read audio data
                if (bytesRead > 0) {
                    audioByteStream.write(buffer, 0, bytesRead); // Write the read audio data to the byte stream
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR - Could not read audio data");
            e.printStackTrace();
        }
    }

    /**
     * Encodes the audio data to a suitable format (WAV).
     *
     * @param audioData The audio data to be encoded.
     * @return The encoded audio data as a byte array, or null if encoding fails.
     */
    private byte[] encodeAudioData(byte[] audioData) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // Create an output stream for encoding
            AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), audioFormat, audioData.length / audioFormat.getFrameSize());
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream); // Write the audio data to the
                                                                                          // output stream as WAV
            return outputStream.toByteArray(); // Return the encoded audio data as a byte array
        } catch (IOException e) {
            System.out.println("ERROR - Could not encode audio data");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Displays a voice note in the chat window.
     *
     * @param message The message containing the voice note.
     */
    void displayVoiceNote(Message message) {
        byte[] audioData = message.getAudioContent(); // Get the audio data from the message
        String sender = message.getSender(); // Get the sender of the message
        // Unique filename for voice note based on time created
        String fileName = "voice_note_" + System.currentTimeMillis() + ".wav"; // Generate a unique filename

        VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName); // Create a VoiceNote object
        Hyperlink voiceNoteLink = createVoiceNoteLink(audioData, sender, fileName); // Create a hyperlink for file
        Platform.runLater(() -> voiceNoteContainer.getChildren().add(voiceNoteLink)); // Add the file link to container
    }

    /**
     * Creates a hyperlink for a voice note.
     *
     * @param audioData The audio data of the voice note.
     * @param sender    The sender of the voice note.
     * @param fileName  The filename of the voice note.
     * @return A hyperlink for the voice note.
     */
    Hyperlink createVoiceNoteLink(byte[] audioData, String sender, String fileName) {
        Hyperlink link = new Hyperlink("Voice note from " + sender); // Create a hyperlink with the sender's name
        link.setOnAction(event -> {
            VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName); // Create a VoiceNote object
            voiceNote.play(); // Play the voice note
        });
        return link;
    }
}