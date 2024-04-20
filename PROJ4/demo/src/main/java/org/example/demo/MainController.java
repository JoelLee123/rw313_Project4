package org.example.demo;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The MainController class is responsible for handling the user interface
 * interactions on the login screen of the chat application.
 */
public class MainController {
    @FXML
    private TextField InputIP;
    @FXML
    private TextField InputUsername;
    @FXML
    private Button btnJoinServer;

    /**
     * Handles the action of the "Join Server" button being clicked.
     * It attempts to connect to the server and opens the chat window if successful.
     *
     * @param event The event that triggered the method call.
     * @throws Exception If there is an error during the connection or UI loading.
     */
    public void btnJoinServerClicked(ActionEvent event) throws Exception {
        String username = InputUsername.getText().trim();
        String ip = InputIP.getText().trim();
        VoIPManager voIPClient = new VoIPManager();
        try {
            Socket socket = new Socket(ip, 4044);
            Stage stage2 = new Stage();

            stage2.setTitle("Messenger");

            FXMLLoader fxmlLoader2 = new FXMLLoader(getClass().getResource("ChatGUI.fxml"));
            Parent root = fxmlLoader2.load();

            stage2.setScene(new Scene(root));
            stage2.show();

            ChatGuiController controller = fxmlLoader2.getController();
            Client client = new Client(socket, username, controller, voIPClient);

            controller.setClient(client);
            controller.setVoipClient(voIPClient);
            controller.setUsername(username);
            client.setChatStage(stage2);

            // Hide the main window after opening the chat window
            ((Node) (event.getSource())).getScene().getWindow().hide();

        } catch (UnknownHostException e) {
            showAlert("Connection Error", "Could not connect to server. Check the IP address and try again.");
            restartLogin();
        } catch (IOException e) {
            showAlert("Connection Error", "Could not connect to server. Server might be down.");
            restartLogin();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void restartLogin() {
        Platform.runLater(() -> {
            try {
                Stage currentStage = (Stage) btnJoinServer.getScene().getWindow();
                currentStage.close();

                FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("MainController.fxml"));
                Parent root = fxmlLoader.load();
                Stage stageRestart = new Stage();
                stageRestart.setTitle("Login");
                stageRestart.setScene(new Scene(root));
                stageRestart.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
