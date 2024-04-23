package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.net.*;

/**
 * Manages individual client connections to the server, handling message sending
 * and receiving.
 */
public class ClientHandler implements Runnable {
    // Static list of all active client handlers
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String clientUsername;

    /**
     * Initializes the client handler with a socket and sets up streams.
     *
     * @param socket the socket representing the client connection
     * @throws IOException            if an I/O error occurs when creating the input
     *                                and output streams
     * @throws ClassNotFoundException if the class of a serialized object cannot be
     *                                found
     */
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());

            // Read the username sent by client
            Message usernameMessage = (Message) objectInputStream.readObject();
            this.clientUsername = usernameMessage.getContent();

            // Check if username is already taken
            if (!Server.activeUsernames.add(this.clientUsername)) {
                objectOutputStream
                        .writeObject(new Message("login", "SERVER", null, "Username is already taken.", false));
                objectOutputStream.flush();
                closeEverything(socket, objectInputStream, objectOutputStream);
                return;
            }

            clientHandlers.add(this);
            System.out.println(clientUsername + " has connected.");
            broadcastMessage(
                    new Message("broadcast", "SERVER", null, clientUsername + " has entered the chat.", false));
                    Server.updateActiveUserList();
        Server.updateClientActivity(clientUsername + " has connected!");

        } catch (IOException | ClassNotFoundException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Listens for messages from the connected client and processes them
     * accordingly.
     */
    @Override
    public void run() {
        Message messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = (Message) objectInputStream.readObject();
                processMessage(messageFromClient);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(clientUsername + " has disconnected!");
                broadcastMessage(
                        new Message("broadcast", "SERVER", null, clientUsername + " has left the chat.", false));
                Server.updateActiveUserList();
                Server.updateClientActivity(clientUsername + " has disconnected!");
                break;
            }
        }
        removeClientHandler();
    }

    private void processMessage(Message message) throws IOException {
        if (!message.getIsAudio()) {
            if (message.getContent().equals("/leave")) {
                handleLeave();
            } else if (message.getType().equals("private")) {
                sendPrivateMessage(message);
            } else if (message.getType().equals("call")) {
                sendPrivateMessage(message);
            } else {
                broadcastMessage(message);
            }
        } else {
            if (message.getType().equals("private"))
                sendPrivateMessage(message);
            else
                broadcastMessage(message);
        }
    }

    /**
     * Sends a private message to a specified recipient.
     *
     * @param message the message to be sent privately
     */
    public void sendPrivateMessage(Message message) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientUsername.equals(message.getRecipient())) {
                try {
                    clientHandler.objectOutputStream.writeObject(message);
                    clientHandler.objectOutputStream.flush();
                } catch (IOException e) {
                    closeEverything(clientHandler.socket, clientHandler.objectInputStream,
                            clientHandler.objectOutputStream);
                }
                return;
            }
        }
        try {
            // Inform sender if the recipient is not found
            objectOutputStream.writeObject(new Message("private", "SERVER", clientUsername,
                    "User '" + message.getRecipient() + "' not found.", false));
            objectOutputStream.flush();
        } catch (IOException e) {

        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     *
     * @param message the message to be broadcasted
     */
    public void broadcastMessage(Message message) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (!clientHandler.clientUsername.equals(clientUsername)) {
                try {
                    clientHandler.objectOutputStream.writeObject(message);
                    clientHandler.objectOutputStream.flush();
                } catch (IOException e) {
                    closeEverything(clientHandler.socket, clientHandler.objectInputStream,
                            clientHandler.objectOutputStream);
                }
            }
        }
    }

    /**
     * Handles the "/leave" command by disconnecting the client.
     */
    private void handleLeave() {
        System.out.println(clientUsername + " has disconnected!");
        Server.updateActiveUserList();
        Server.updateClientActivity(clientUsername + " has disconnected!");
        broadcastMessage(new Message("broadcast", "SERVER", null, clientUsername + " has left the chat.", false));
    }

    /**
     * Removes this ClientHandler from the list of active handlers and the set of
     * active usernames.
     */
    public void removeClientHandler() {
        Server.activeUsernames.remove(this.clientUsername);
        clientHandlers.remove(this);
        Server.updateActiveUserList();
    }

    /**
     * Closes the socket and the input/output streams associated with this client.
     *
     * @param socket             the socket to be closed
     * @param objectInputStream  the input stream to be closed
     * @param objectOutputStream the output stream to be closed
     */
    public void closeEverything(Socket socket, ObjectInputStream objectInputStream,
            ObjectOutputStream objectOutputStream) {
        removeClientHandler();
        try {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
