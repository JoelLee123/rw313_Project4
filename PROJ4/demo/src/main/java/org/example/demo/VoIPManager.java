package org.example.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * This class manages VoIP (Voice over IP) communications using multicast
 * addressing.
 * It handles the setup of audio input and output, manages network
 * communications,
 * and maintains a list of ongoing calls.
 */
public class VoIPManager {
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private MulticastSocket socket;
    private InetAddress activeInet;
    private NetworkInterface networkInterface;
    private Set<InetAddress> localAddresses;
    private int port = 42069; // Default multicast port, adjust as needed

    private static final double THRESHOLD_VALUE = 1000;
    private static final String BASE_ADDRESS = "ff02::1:";

    /**
     * Constructs a new VoIPManager, initializing network interfaces and local
     * addresses.
     */
    public VoIPManager() {
        try {
            audioFormat = getAudioFormat();
            networkInterface = findMulticastInterface();
            localAddresses = new HashSet<>();
            populateLocalAddresses();
        } catch (SocketException e) {
            System.err.println("Error initializing VoIPManager: " + e.getMessage());
        }
    }

    /**
     * Populates the set of local addresses that are active and not loopback.
     */
    private void populateLocalAddresses() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        localAddresses.add(interfaceAddress.getAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error populating local addresses: " + e.getMessage());
        }
    }

    /**
     * Returns a standard AudioFormat used for VoIP communications.
     * 
     * @return configured AudioFormat object.
     */
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F; // Standard for VoIP
        int sampleSizeInBits = 16; // High-quality audio
        int channels = 1; // Mono, sufficient for voice
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    /**
     * Starts a VoIP communication session for a specified address.
     * 
     * @param address The multicast address to use for this session.
     */
    public void start() {
        try {
            this.activeInet = InetAddress.getByName("ff02::1");
            setupMulticast(this.activeInet);
            startCommunication();
        } catch (IOException | LineUnavailableException e) {
            System.err.println("Error starting VoIP communication: " + e.getMessage());
        }
    }

    /**
     * Starts a call with a specific user, allocating a unique multicast address.
     * 
     * @param username The user's identifier to start a call with.
     */
    public void startCall(String sender) {
        try {
            this.activeInet = allocateMulticastAddress();
            this.activeInet = InetAddress.getByName("ff02::1:3");
            setupMulticast(this.activeInet);
            startCommunication();
            Server.addActiveCall(sender, this.activeInet);
            System.out.println(Server.activeCalls);
            System.out.println("Call started for: " + sender + " at " + this.activeInet);
        } catch (IOException | LineUnavailableException e) {
            System.err.println("Error starting call" + e.getMessage());
        }
    }

    /**
     * Accepts a call by joining the multicast group associated with the username.
     * 
     * @param username The username whose call to join.
     */
    public void acceptCall(String username) {
        try {
            System.out.println(Server.activeCalls);
            // this.activeInet = (InetAddress) Server.getActiveCall(username);
            this.activeInet = InetAddress.getByName("ff02::1:3");
            if (this.activeInet != null) {
                setupMulticast(this.activeInet);
                startCommunication();
                // System.out.println("Joined " + username + " at " + this.activeInet);
            } else {
                System.err.println("No active call found for username: " + username);
            }
        } catch (IOException | LineUnavailableException e) {
            System.err.println("Error accepting call for username " + username + ": " + e.getMessage());
        }
    }

    /**
     * Denies an ongoing call, removing the association from active calls and
     * freeing up the address.
     * 
     * @param username The user whose call is to be denied.
     */
    public void denyCall(String username) {
        InetAddress address = Server.removeActiveCall(username);
        if (address != null) {
            Server.availableAddresses.add(address);
            System.out.println("Call with " + username + " has been denied and the address has been freed.");
        } else {
            System.err.println("No active call with username: " + username + " found to deny.");
        }
    }

    /**
     * Allocates a unique multicast address from the pool of available addresses.
     * 
     * @return The allocated InetAddress.
     */
    public static InetAddress allocateMulticastAddress() {
        if (!Server.availableAddresses.isEmpty()) {
            Iterator<InetAddress> it = Server.availableAddresses.iterator();
            InetAddress allocated = it.next();
            it.remove();
            return allocated;
        } else {
            System.err.println("No available multicast addresses.");
            return null;
        }
    }

    /**
     * Sets up the multicast socket and joins the multicast group.
     * 
     * @param address The multicast address to join.
     * @throws IOException if an I/O error occurs.
     */
    private void setupMulticast(InetAddress address) throws IOException {
        socket = new MulticastSocket(port);
        socket.joinGroup(new InetSocketAddress(address, port), networkInterface);
    }

    /**
     * Starts the communication threads for capturing and receiving audio.
     * 
     * @throws LineUnavailableException if a line cannot be opened.
     */
    private void startCommunication() throws LineUnavailableException {
        setupAudioDevices();
        new Thread(this::captureAudio).start();
        new Thread(this::receiveAudio).start();
    }

    /**
     * Stops communication for a specific user, cleans up resources, and makes the
     * multicast address available again.
     * 
     * @param username The identifier of the user whose communication is to be
     *                 stopped.
     */
    public void leaveCall(String username) {
        InetAddress address;

        if (username != null) {
            address = Server.removeActiveCall(username);
            address = this.activeInet;
        } else {
            address = this.activeInet;
        }

        if (address != null) {
            try {
                if (socket != null) {
                    socket.leaveGroup(new InetSocketAddress(address, port), networkInterface); // leave group
                    socket.close();
                    socket = null;
                }
                if (microphone != null) {
                    microphone.stop(); // stop microphone
                    microphone.close();
                    microphone = null;
                }
                if (speakers != null) {
                    speakers.stop(); // stop speakers
                    speakers.close();
                    speakers = null;
                }
                if (username != null) {
                    Server.availableAddresses.add(address); // Add the address back to the pool of available addresses
                    System.out.println("Stopped call and released resources for " + username);
                }

            } catch (IOException e) {
                System.err.println("Error stopping communication for " + username + ": " + e.getMessage());
            }
        } else {
            System.err.println("No active call with username: " + username + " to stop.");
        }
    }

    /**
     * Sets up audio devices for capture and playback.
     * 
     * @throws LineUnavailableException if a line for audio capture or playback
     *                                  cannot be opened.
     */
    private void setupAudioDevices() throws LineUnavailableException {
        microphone = openMicrophone();
        speakers = openSpeakers();
        speakers.start();
        microphone.start();
    }

    /**
     * Opens the microphone line for audio capture.
     * 
     * @return The opened TargetDataLine for the microphone.
     * @throws LineUnavailableException if the microphone line is not supported or
     *                                  cannot be opened.
     */
    private TargetDataLine openMicrophone() throws LineUnavailableException {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(micInfo)) {
            throw new LineUnavailableException("Microphone line not supported.");
        }
        TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(micInfo);
        mic.open(audioFormat); // open mic
        return mic;
    }

    /**
     * Opens the speakers line for audio output.
     * 
     * @return The opened SourceDataLine for the speakers.
     * @throws LineUnavailableException if the speakers line is not supported or
     *                                  cannot be opened.
     */
    private SourceDataLine openSpeakers() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(speakerInfo)) {
            throw new LineUnavailableException("Speakers line not supported.");
        }
        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakers.open(audioFormat); // open speakers
        return speakers;
    }

    /**
     * Captures audio from the microphone and sends it over the network.
     */
    private void captureAudio() {
        byte[] buffer = new byte[1024];
        while (microphone != null && microphone.isOpen()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);

            // Simple volume check to see if buffer is above the threshold
            // if (isAboveThreshold(buffer, bytesRead)) {
            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, activeInet, port);
            try {
                if (socket != null)
                    socket.send(packet); // send packets if not null
            } catch (IOException | NullPointerException e) {

            }
            // }
        }
    }

    private boolean isAboveThreshold(byte[] audioBuffer, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            // Combine two bytes to form a 16-bit sample
            int sample = (audioBuffer[i + 1] << 8) | (audioBuffer[i] & 0xFF);
            sum += Math.abs(sample); // Sum the absolute values of the samples
        }
        // Determine the average volume of this buffer
        double average = sum / (bytesRead / 2.0);
        // Return true if the average is above the threshold
        return average > THRESHOLD_VALUE;
    }

    /**
     * Receives audio from the network and plays it through the speakers.
     */
    private void receiveAudio() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (speakers != null && speakers.isOpen()) {
            try {
                if (socket != null) {
                    socket.receive(packet);
                    if (!isLocalPacket(packet)) {
                        speakers.write(packet.getData(), 0, packet.getLength());
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public void muteMic() {
        System.out.println("muted");
        this.microphone.stop();
    }

    public void unmuteMic() {
        System.out.println("unmuted");
        this.microphone.start();
    }

    /**
     * Checks if a received packet is from a local address.
     * 
     * @param packet The received DatagramPacket.
     * @return true if the packet's source address is local, false otherwise.
     */
    private boolean isLocalPacket(DatagramPacket packet) {
        // System.out.println(localAddresses);
        // return localAddresses.contains(packet.getAddress());
        return false;
    }

    /**
     * Finds a network interface that supports multicast and is not a loopback
     * interface.
     * 
     * @return The found NetworkInterface.
     * @throws SocketException if no suitable interface is found.
     */
    public static NetworkInterface findMulticastInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback()) {
                return ni;
            }
        }
        throw new SocketException("No suitable interface found");
    }
}
