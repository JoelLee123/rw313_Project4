package org.example.demo;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class VoIPManager {
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private MulticastSocket socket;
    private InetAddress groupAddress;
    private NetworkInterface networkInterface;
    private int port = 42069; // Example multicast port, adjust as needed

    public VoIPManager() throws IOException {
        audioFormat = getAudioFormat();
        networkInterface = findMulticastInterface();
        // Microphone and speakers are not opened here; they're managed dynamically.
    }

    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F; // Standard for VoIP
        int sampleSizeInBits = 16; // High-quality audio
        int channels = 1; // Mono, sufficient for voice
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void start() throws IOException, LineUnavailableException {
        System.out.println("A");
        setupAudioDevices(); // Setup microphone and speakers
        System.out.println("B");
        setupMulticast(); // Setup multicast connection
        System.out.println("C");

        speakers.start();
        microphone.start();
        Thread captureThread = new Thread(this::captureAudio);
        captureThread.start();
        Thread receiveThread = new Thread(this::receiveAudio);
        receiveThread.start();
    }

    public void stop() {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
        if (speakers != null) {
            speakers.stop();
            speakers.close();
            speakers = null;
        }
        if (socket != null) {
            try {
                socket.leaveGroup(new InetSocketAddress(groupAddress, port), networkInterface);
            } catch (IOException e) {
                System.err.println("Failed to leave multicast group: " + e.getMessage());
            }
            socket.close();
            socket = null;
        }
    }

    private void setupAudioDevices() throws LineUnavailableException {
        microphone = openMicrophone();
        speakers = openSpeakers();
    }

    private TargetDataLine openMicrophone() throws LineUnavailableException {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(micInfo)) {
            throw new LineUnavailableException("Microphone line not supported.");
        }
        TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(micInfo);
        mic.open(audioFormat);
        return mic;
    }

    private SourceDataLine openSpeakers() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(speakerInfo)) {
            throw new LineUnavailableException("Speakers line not supported.");
        }
        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakers.open(audioFormat);
        return speakers;
    }

    private void setupMulticast() throws IOException {
        if (networkInterface == null) {
            throw new IOException("No suitable network interface found for multicast.");
        }
        socket = new MulticastSocket(port);
        // 230.0.0.1 - used by others
        groupAddress = InetAddress.getByName("ff02::1");
        socket.joinGroup(new InetSocketAddress(groupAddress.getHostAddress(), port), networkInterface);
    }

    private void captureAudio() {
        byte[] buffer = new byte[1024]; // Adjust buffer size based on requirements
        while (microphone != null && microphone.isOpen()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, groupAddress, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Error sending audio packet: " + e.getMessage());
            }
        }
    }

    private void receiveAudio() {
        byte[] buffer = new byte[1024]; // Ensure buffer size matches the send buffer
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (speakers != null && speakers.isOpen()) {
            try {
                socket.receive(packet);
                // Check if the packet's source is not the local address to prevent feedback
                if (!isLocalPacket(packet)) {
                    speakers.write(packet.getData(), 0, packet.getLength());
                }
            } catch (IOException e) {
                System.err.println("Error receiving audio packet: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if the given packet originated from any of the local network
     * interfaces.
     * 
     * @param packet the DatagramPacket to check
     * @return true if the packet originated locally, false otherwise
     */
    private boolean isLocalPacket(DatagramPacket packet) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        InetAddress localAddress = interfaceAddress.getAddress();
                        if (packet.getAddress().equals(localAddress)) {
                            return true;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static NetworkInterface findMulticastInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback()) {
                return ni;
            }
        }
        return null; // No suitable interface found
    }
}
