package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.sound.sampled.AudioFileFormat;

import javax.sound.sampled.*;
import java.io.*;

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
    private VBox voiceNoteContainer;

    /* Audio related variables */
    private TargetDataLine audioLine;
    private AudioFormat audioFormat;
    private ByteArrayOutputStream audioByteStream;
    private byte[] audioData;

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

    /**
     * Handles the action of the "Send Message" button being clicked.
     * It sends the message to the server and displays it in the chat window.
     *
     * @param event The event that triggered the method call.
     */
    @FXML
    void btnSendMessageClicked(ActionEvent event) {
        String messageText = InputMessage.getText();
        System.out.println(messageText);
        Message message;

        if ("/leave".equalsIgnoreCase(messageText.trim())) {
            message = new Message("command", username, null, "/leave", false);
            this.displayMessage(message);

        } else if (messageText.startsWith("/w ")) {
            String[] parts = messageText.split(" ", 3);
            if (parts.length >= 3) {
                String recipient = parts[1];
                messageText = parts[2];
                message = new Message("private", username, recipient, messageText.trim(), false);
                this.client.sendMessage(message);
                this.displayMessage(message);
            }

        } else if (!messageText.trim().isEmpty()) {
            message = new Message("broadcast", username, null, messageText.trim(), false);
            this.client.sendMessage(message);
            this.displayMessage(message);
        }
        InputMessage.clear();
    }

    @FXML
    void btnStartRecordingClicked(ActionEvent event) {

        try {
            //Initialize the audioFormat and audioLine
            audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.print("Data line not supported");
            }

            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open();
            audioLine.start();  //The start of the recording

            //Initialize the audioByteSteam
            audioByteStream = new ByteArrayOutputStream();

            //We want to start recording on a separate thread so that recording audio does not affect the other parts of the program
            new Thread(() -> {
                System.out.println("Start Recording...");
                recordAudio();
            }).start();

            System.out.println("Start Recording Clicked");
        } catch (LineUnavailableException e) {
            System.out.println("ERROR - Could not startRecording");
            e.printStackTrace();
        }
    }

    private void recordAudio() {
        byte[] buffer = new byte[4096];
        int bytesRead;

        try {
            while (recording && (bytesRead = audioLine.read(buffer, 0, buffer.length)) != -1) {
                System.out.println("Reading in the bytes");
                if (bytesRead > 0) {
                    audioByteStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR - Could not read audio data");
            e.printStackTrace();
        }

        System.out.println("Recording finished");
    }

    @FXML
    void btnEndRecordingClicked(ActionEvent event) {
        System.out.println("End Recording Clicked");

        //Stop audio recording
        recording = false;
        audioLine.stop();
        audioLine.close();

        //Encode audio data
        audioData = audioByteStream.toByteArray();


        byte[] encodedAudioData = encodeAudioData(audioData);
        System.out.println("Data encoded");

        //PLAYBACK FEATURE - USED FOR DEBUGGING!
        System.out.println("Size of encoded audio in bytes: " + encodedAudioData.length);
        playAudio(encodedAudioData);

        //Create a new audio message and send it to the server
        Message audioMessage = new Message("audio",username, null, encodedAudioData, true);
        System.out.println("Message object created");
        client.sendMessage(audioMessage);
        System.out.println("Message sent");
    }

    private void playAudio(byte[] audioData) {
        try {
            // Create an AudioInputStream from the recorded audio data
            AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), audioFormat,
                    audioData.length / audioFormat.getFrameSize());

            // Create a Clip to play back the audio
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Start playback
            clip.start();

            // Wait for playback to finish
            clip.drain();
            clip.close();
        } catch (Exception e) {
            System.out.println("ERROR - Could not play audio");
            e.printStackTrace();
        }
    }

    private byte[] encodeAudioData(byte[] audioData) {
        try {
            // Encode the audioData to a suitable format (e.g., MP3)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), audioFormat, audioData.length / audioFormat.getFrameSize()
            );
            //Decided to use WAV as audio quality is better - also easier to work with
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            System.out.println("ERROR - Could not encode audio data");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Displays a message in the chat window.
     *
     * @param message The message to be displayed.
     */
    public void displayMessage(Message message) {
        Platform.runLater(() -> {
            if (!message.getIsAudio()) {    //If not audio -> Must be text
                System.out.println("Handling text data");
                String formattedMessage = formatMessage(message);
                MessageOutput.appendText(formattedMessage + "\n");
            } else if (message.getIsAudio()) {
                System.out.println("Handling audio data");
                displayVoiceNote(message);
            }
        });
    }

    /**
     * Displaying the voice note as something a client can click on
     * @param message The message to be displayed -> voice note
     */
    void displayVoiceNote(Message message) {
        System.out.println("IN display voice Note");

        byte[] audioData = message.getAudioContent();
        String sender = message.getSender();
        //Unique filename for voice note based on time created
        String fileName = "voice_note_" + System.currentTimeMillis() + ".wav";

        VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName);

        System.out.println("Got here");

        Hyperlink voiceNoteLink = createVoiceNoteLink(audioData, sender, fileName);
        Platform.runLater(() -> voiceNoteContainer.getChildren().add(voiceNoteLink));

        System.out.println("Reachable");
    }

    Hyperlink createVoiceNoteLink(byte[] audioData, String sender, String fileName) {
        System.out.println("Created voice note link!");
        Hyperlink link = new Hyperlink("Voice note from " + sender);
        link.setOnAction(event -> {
            VoiceNote voiceNote = new VoiceNote(audioData, sender, fileName);
            voiceNote.play();
        });
        return link;
    }

    /*void playVoiceNote(String fileName) {
        Media media = new Media(new File(fileName).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
    } */

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
                formattedMessage = "[You] " + message.getTextContent();
            } else {
                formattedMessage = "[" + message.getSender() + "] " + message.getTextContent();
            }
        } else if (message.getType().equals("private")) {
            if (message.getSender().equals(username)) {
                formattedMessage = "[You] (whisper to " + message.getRecipient() + ") " + message.getTextContent();
            } else {
                formattedMessage = "[" + message.getSender() + "] (whisper) " + message.getTextContent();
            }
        } else if (message.getType().equals("command") && message.getTextContent().equalsIgnoreCase("/leave")) {
            formattedMessage = "You have left the chat.";
            this.client.closeEverythingHelper();
            Platform.exit();
        }
        return formattedMessage;
    }
}
