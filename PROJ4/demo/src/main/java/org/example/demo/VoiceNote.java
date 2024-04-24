package org.example.demo;

import javafx.scene.media.*;
import java.io.File;
import java.io.FileOutputStream;

/**
 * VoiceNote manages the storage and playback of audio data for voice messages
 * in a chat application.
 */
public class VoiceNote {

    private byte[] audioData; // The raw audio data for the voice note
    private String sender; // The sender of the voice note
    private String fileName; // The filename under which the voice note will be stored
    private Media media; // Media object to handle audio data
    private MediaPlayer mediaPlayer; // MediaPlayer to control playback of the voice note

    /**
     * Constructs a VoiceNote with specified audio data, sender, and file name.
     * 
     * @param audioData The raw audio data of the voice note.
     * @param sender    The username of the sender of the voice note.
     * @param fileName  The filename for storing the voice note.
     */
    public VoiceNote(byte[] audioData, String sender, String fileName) {
        this.audioData = audioData;
        this.sender = sender;
        this.fileName = fileName;

        String currentDir = System.getProperty("user.dir");
        String projectPath = "/PROJ4/demo/src/main/java/org/example/demo/Notes/";
        String relativePath = currentDir + projectPath + fileName;
        saveFile(relativePath);
        System.out.println("OK COOL");
        this.media = new Media(new File(relativePath).toURI().toString());
        System.out.println("Fine");
        this.mediaPlayer = new MediaPlayer(media);

    }

    /**
     * Retrieves the raw audio data of the voice note.
     * 
     * @return An array of bytes representing the audio data.
     */
    public byte[] getAudioData() {
        return audioData;
    }

    /**
     * Retrieves the sender of the voice note.
     * 
     * @return A string representing the username of the sender.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Retrieves the file name under which the voice note is stored.
     * 
     * @return A string representing the file name of the voice note.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Saves the voice note to the specified file path.
     * 
     * @param absolutePath The absolute path where the file should be saved.
     */
    void saveFile(String absolutePath) {
        System.out.println("Made it here");

        try (FileOutputStream fos = new FileOutputStream(absolutePath)) {
            System.out.println("Break just before write?");
            fos.write(audioData);
            System.out.println("Audio file saved to: " + absolutePath);
        } catch (Exception e) {
            System.out.println("ERROR - Could not write wav file to directory");
            e.printStackTrace();
        }
    }

    /**
     * Plays the voice note using the associated MediaPlayer.
     */
    public void play() {
        // Play voice note using MediaPlayer
        mediaPlayer.play();
        // Server.updateClientActivity("Client playing voice note."); // Un-comment this
        // line if server activity updates are required
    }
}
