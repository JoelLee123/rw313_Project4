package org.example.demo;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoIPManager {
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private MulticastSocket socket;
    private InetAddress groupAddress;
    private NetworkInterface networkInterface;
    private Set<InetAddress> localAddresses;
    private int port = 42069; // Example multicast port, adjust as needed
    private Map<String, InetAddress> userGroupMap = new ConcurrentHashMap<>();

    public VoIPManager() throws IOException {
        audioFormat = getAudioFormat();
        networkInterface = findMulticastInterface();
        localAddresses = new HashSet<>();
        populateLocalAddresses();
    }

    private void populateLocalAddresses() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp() && !ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    localAddresses.add(interfaceAddress.getAddress());
                }
            }
        }
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
        // Set up a default multicast group for all users
        groupAddress = InetAddress.getByName("239.255.0.1"); // Default multicast address for group calls
        setupMulticast(groupAddress);
        startCommunication();
    }

    public void startCallWithUser(String username, String targetMulticastAddress)
            throws IOException, LineUnavailableException {
        // Set up a specific multicast group for a call between specific users
        if (!userGroupMap.containsKey(username)) {
            InetAddress address = InetAddress.getByName(targetMulticastAddress);
            userGroupMap.put(username, address);
            setupMulticast(address);
            startCommunication();
        }
    }

    private void setupMulticast(InetAddress address) throws IOException {
        socket = new MulticastSocket(port);
        socket.joinGroup(new InetSocketAddress(address, port), networkInterface);
        this.groupAddress = address; // Update the current group address
    }

    private void startCommunication() throws LineUnavailableException {
        setupAudioDevices(); // Setup microphone and speakers

        speakers.start();
        microphone.start();
        new Thread(this::captureAudio).start();
        new Thread(this::receiveAudio).start();
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
                socket.close();
                socket = null;
            } catch (IOException e) {
                System.err.println("Failed to leave multicast group: " + e.getMessage());
            }
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

    private void captureAudio() {
        byte[] buffer = new byte[1024];
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
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (speakers != null && speakers.isOpen()) {
            try {
                socket.receive(packet);
                if (!isLocalPacket(packet)) {
                    speakers.write(packet.getData(), 0, packet.getLength());
                }
            } catch (IOException e) {
                System.err.println("Error receiving audio packet: " + e.getMessage());
            }
        }
    }

    private boolean isLocalPacket(DatagramPacket packet) {
        return localAddresses.contains(packet.getAddress());
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
