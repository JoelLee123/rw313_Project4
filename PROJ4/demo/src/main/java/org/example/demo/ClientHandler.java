package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * The ClientHandler class is responsible for managing individual client
 * connections
 * to the server, including sending and receiving messages.
 */
public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String clientUsername;

    /**
     * Constructs a ClientHandler instance with a specified socket.
     * Initializes the streams and sets up the client connection.
     *
     * @param socket The socket representing the client connection.
     */
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());

            Message usernameMessage = (Message) objectInputStream.readObject();
            this.clientUsername = usernameMessage.getContent();

            if (!Server.activeUsernames.add(this.clientUsername)) {
                objectOutputStream
                        .writeObject(new Message("login", "SERVER", null, "Username is already taken.", false));
                objectOutputStream.flush();
                Server.activeUsernames.remove(this.clientUsername);
                closeEverything(socket, objectInputStream, objectOutputStream);
                return;
            }

            clientHandlers.add(this);
            Server.activeUsernames.add(this.clientUsername);
            System.out.println(clientUsername + " has connected.");
            broadcastMessage(
                    new Message("broadcast", "SERVER", null, clientUsername + " has entered the chat.", false));

        } catch (IOException | ClassNotFoundException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Listens for messages from the connected client and processes them.
     */
    @Override
    public void run() {
        Message messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = (Message) objectInputStream.readObject();

                if (!messageFromClient.getIsAudio()) {
                    System.out.println("Message is text");
                    // OBJECT RECEIVED REFLECTS A TEXT MESSAGE -> AVOIDS NULL POINTER EXCEPTIONS
                    if (messageFromClient.getContent().equals("/leave") || messageFromClient == null) {
                        System.out.println(clientUsername + " has disconnected!");
                        broadcastMessage(new Message("broadcast", "SERVER", null,
                                clientUsername + " has left the chat.", false));
                        break;
                    } else if (messageFromClient.getType().equals("private")) {
                        sendPrivateMessage(messageFromClient);
                    } else {
                        broadcastMessage(messageFromClient);
                    }
                } else {
                    // OBJECT RECEIVED REFLECTS AUDIO
                    System.out.println("Object in run method is an audio object");
                    if (messageFromClient.getType().equals("private")) {
                        sendPrivateMessage(messageFromClient);
                    } else {
                        broadcastMessage(messageFromClient);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println(clientUsername + " has disconnected!");
                broadcastMessage(
                        new Message("broadcast", "SERVER", null, clientUsername + " has left the chat.", false));
                break;
            }
        }
        removeClientHandler();
    }

    /**
     * Sends a private message to the specified recipient.
     *
     * @param message The message to be sent.
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
        // If the target user was not found, inform the sender
        try {
            objectOutputStream.writeObject(new Message("private", "SERVER", clientUsername,
                    "User '" + message.getRecipient() + "' not found.", false));
            objectOutputStream.flush();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     *
     * @param message The message to be broadcasted.
     */
    public void broadcastMessage(Message message) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.objectOutputStream.writeObject(message);
                    clientHandler.objectOutputStream.flush();
                }
            } catch (IOException e) {
                closeEverything(clientHandler.socket, clientHandler.objectInputStream,
                        clientHandler.objectOutputStream);
                removeClientHandler();
            }
        }
    }

    /**
     * Removes this ClientHandler from the list of active handlers and the set of
     * active usernames.
     */
    public void removeClientHandler() {
        Server.activeUsernames.remove(this.clientUsername);
        clientHandlers.remove(this);
    }

    /**
     * Closes the socket and input/output streams associated with this client
     * handler.
     *
     * @param socket             The socket to be closed.
     * @param objectInputStream  The input stream to be closed.
     * @param objectOutputStream The output stream to be closed.
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
