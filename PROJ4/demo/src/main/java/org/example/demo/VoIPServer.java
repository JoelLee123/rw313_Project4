package org.example.demo;

import java.net.*;
import java.util.concurrent.*;
import java.io.*;

public class VoIPServer {
    private DatagramSocket socket;
    private ExecutorService pool;

    public VoIPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
        pool = Executors.newCachedThreadPool();
    }

    public void acceptConnections() {
        byte[] buffer = new byte[1024]; // Adjust buffer size based on expected packet size
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                socket.receive(packet); // Receive packet
                pool.execute(new VoIPSessionHandler(socket, packet)); // Handle packet
            } catch (IOException e) {
                System.out.println("Socket error: " + e.getMessage());
                break;
            }
        }
    }
}

class VoIPSessionHandler implements Runnable {
    private DatagramSocket socket;
    private DatagramPacket packet;

    public VoIPSessionHandler(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }

    public void run() {
        // Here you could add logic to forward the packet to other clients or process it
        System.out.println("Received packet from " + packet.getAddress().toString());
        // Echo the packet back to the sender as an example
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending packet: " + e.getMessage());
        }
    }
}
