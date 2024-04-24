package org.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * ServerController handles the user interface updates on the server side for a
 * chat application.
 * It provides methods to update the active user list and append activities to
 * the client activity log.
 */
public class ServerController {

    @FXML
    private TextArea ActiveUsersTA; // Text area to display active users

    @FXML
    private TextArea ClientActivityTA; // Text area to display client activity logs

    /**
     * Updates the text area that lists active users.
     * 
     * @param activeUsers A string containing the usernames of active users,
     *                    separated by newlines.
     */
    public void updateActiveUsers(String activeUsers) {
        ActiveUsersTA.setText(activeUsers);
    }

    /**
     * Appends a new activity string to the client activity log.
     * 
     * @param activity A string describing the client's activity.
     */
    public void appendClientActivity(String activity) {
        ClientActivityTA.appendText(activity + "\n");
    }

}
