package org.example.demo;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private static Set<InetAddress> availableAddresses = new HashSet<>();
    private static final String BASE_ADDRESS = "ff02::1:";
    private static Map<String, InetAddress> activeCalls = new ConcurrentHashMap<>();

    static {
        try {
            for (int i = 2; i <= 10; i++) {
                availableAddresses.add(InetAddress.getByName(BASE_ADDRESS + i));
            }
        } catch (UnknownHostException e) {
            System.err.println("Failed to initialise available addresses: " + e.getMessage());
        }
    }

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
            String address = BASE_ADDRESS + 1;
            System.out.println(address);
            activeInet = InetAddress.getByName(BASE_ADDRESS + 1);
            setupMulticast(activeInet);
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
    public void startCall(String username) {
        try {
            activeInet = allocateMulticastAddress();
            setupMulticast(activeInet);
            startCommunication();
        } catch (IOException | LineUnavailableException e) {
            System.err.println("Error starting call with user " + username + ": " + e.getMessage());
        }
    }

    /**
     * Accepts a call by joining the multicast group associated with the username.
     * 
     * @param username The username whose call to join.
     */
    public void acceptCall(String username) {
        try {
            activeInet = activeCalls.get(username);
            if (activeInet != null) {
                setupMulticast(activeInet);
                startCommunication();
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
        InetAddress address = activeCalls.remove(username);
        if (address != null) {
            availableAddresses.add(address);
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
        if (!availableAddresses.isEmpty()) {
            Iterator<InetAddress> it = availableAddresses.iterator();
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
        this.activeInet = address;
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
        if (username != null)
            address = activeCalls.remove(username);
        else
            address = activeInet;

        if (address != null) {
            try {
                if (socket != null) {
                    socket.leaveGroup(new InetSocketAddress(address, port), networkInterface);
                    socket.close();
                    socket = null;
                }
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
                if (username != null) {
                    availableAddresses.add(address); // Add the address back to the pool of available addresses
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
        mic.open(audioFormat);
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
        speakers.open(audioFormat);
        return speakers;
    }

    /**
     * Captures audio from the microphone and sends it over the network.
     */
    private void captureAudio() {
        byte[] buffer = new byte[1024];
        while (microphone != null && microphone.isOpen()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, activeInet, port);
            try {
                if (socket != null)
                    socket.send(packet);
            } catch (IOException e) {
                System.err.println("Error sending audio packet: " + e.getMessage());
            }
        }
    }

    /**
     * Receives audio from the network and plays it through the speakers.
     */
    private void receiveAudio() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (speakers != null && speakers.isOpen()) {
            try {
                if (socket != null) {
                    socket.receive(packet);
                    // if (!isLocalPacket(packet)) {
                    speakers.write(packet.getData(), 0, packet.getLength());
                    // }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * Checks if a received packet is from a local address.
     * 
     * @param packet The received DatagramPacket.
     * @return true if the packet's source address is local, false otherwise.
     */
    private boolean isLocalPacket(DatagramPacket packet) {
        return localAddresses.contains(packet.getAddress());
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
