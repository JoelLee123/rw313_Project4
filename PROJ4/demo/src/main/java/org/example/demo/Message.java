package org.example.demo;

import java.io.Serializable;

/**
 * The Message class represents a message that can be sent between clients and
 * the server.
 * It contains information about the message type, sender, recipient, and
 * content.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String type; // "broadcast" or "private"
    private final String sender;
    private final String recipient; // Used for private messages
    private final String textContent;

    private final byte[] audioContent;
    private final boolean isAudio;

    /**
     * Constructs a new Message instance with the specified parameters.
     *
     * @param type        The type of the message ("broadcast" or "private").
     * @param sender      The username of the sender.
     * @param recipient   The username of the recipient (for private messages).
     * @param textContent The textual content of the message.
     */
    public Message(String type, String sender, String recipient, String textContent, boolean isAudio) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.textContent = textContent;
        this.audioContent = null;
        this.isAudio = isAudio;
    }

    /**
     * Constructs a new Message instance with the specified parameters.
     *
     * @param type         The type of the message ("broadcast" or "private").
     * @param sender       The username of the sender.
     * @param recipient    The username of the recipient (for private messages).
     * @param audioContent The byte array for voicenotes
     * @param textContent  The textual content of the message.
     */
    public Message(String type, String sender, String recipient, byte[] audioContent, boolean isAudio) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.textContent = null; // Voice notes have no text
        this.audioContent = audioContent;
        this.isAudio = isAudio;
    }

    /**
     * Gets the type of the message.
     *
     * @return The message type ("broadcast" or "private").
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the sender's username.
     *
     * @return The sender's username.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Gets the recipient's username (for private messages).
     *
     * @return The recipient's username, or null if the message is not private.
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * Gets the content of the message.
     *
     * @return The textual content of the message.
     */
    public String getContent() {
        return textContent;
    }

    // VOICE NOTE METHODS
    public byte[] getAudioContent() {
        return audioContent;
    }

    public boolean getIsAudio() {
        return isAudio;
    }
}
