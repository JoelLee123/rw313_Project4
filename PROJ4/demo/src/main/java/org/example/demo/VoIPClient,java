package org.example.demo;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class VoIPClient {
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort = 50005; // Example port, adjust as needed

    public VoIPClient() {
        try {
            audioFormat = getAudioFormat();
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakers.open(audioFormat);
            microphone.open(audioFormat);
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost"); // Use actual server IP
        } catch (LineUnavailableException | SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private AudioFormat getAudioFormat() {
        float sampleRate = 8000.0F; // 8000,11025,16000,22050,44100
        int sampleSizeInBits = 16; // 8,16
        int channels = 1; // 1,2
        boolean signed = true; // true,false
        boolean bigEndian = false; // true,false
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void start() {
        speakers.start();
        microphone.start();
        Thread captureThread = new Thread(new CaptureThread());
        captureThread.start();
        Thread receiveThread = new Thread(new ReceiveThread());
        receiveThread.start();
    }

    public void stop() {
        microphone.stop();
        microphone.close();
        speakers.stop();
        speakers.close();
        socket.close();
    }

    // Captures audio from the microphone and sends it as packets
    class CaptureThread implements Runnable {
        public void run() {
            byte[] buffer = new byte[1024]; // Adjust the size based on audio format and desired packet size
            while (microphone.isOpen()) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, serverAddress, serverPort);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Receives packets and plays them back through the speakers
    class ReceiveThread implements Runnable {
        public void run() {
            byte[] buffer = new byte[1024]; // Match the packet size used in the CaptureThread
            while (speakers.isOpen()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        VoIPClient client = new VoIPClient();
        client.start(); // Start the client
    }
}
