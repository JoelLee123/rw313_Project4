package org.example.demo;

import javafx.scene.media.*;
import java.io.File;
import java.io.FileOutputStream;

public class VoiceNote {

    private byte[] audioData;
    private String sender;
    private String fileName;
    private Media media;
    private MediaPlayer mediaPlayer;


    public VoiceNote(byte[] audioData, String sender, String fileName) {
        this.audioData = audioData;
        this.sender = sender;
        this.fileName = fileName;

        String absolutePath = "/home/joel/CS313/Proj1_Proj4/PROJ4/demo/src/main/java/org/example/demo/" + fileName;
        saveFile(absolutePath);
        System.out.println("OK COOL");
        this.media = new Media(new File(absolutePath).toURI().toString());
        System.out.println("Fine");
        this.mediaPlayer = new MediaPlayer(media);
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }
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

    public void play() {
        //Play voice note using MediaPlayer
        mediaPlayer.play();
    }
}
