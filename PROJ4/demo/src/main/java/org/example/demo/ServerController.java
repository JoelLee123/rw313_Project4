package org.example.demo;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerController {

    @FXML
    private TextArea ActiveUsersTA;

    @FXML
    private TextArea ClientActivityTA;

    // can rather change to the arraylist of active users
    public void updateActiveUsers(String activeUsers) {
        ActiveUsersTA.setText(activeUsers);
    }

    public void appendClientActivity(String activity) {
        ClientActivityTA.appendText(activity + "\n");
    }

}
