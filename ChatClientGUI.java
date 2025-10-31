import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ChatClientGUI extends Application {

    private VBox messageBox;
    private TextField inputField;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;

    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 1234;

    @Override
    public void start(Stage primaryStage) {
        // --- Login Stage ---
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Chat Login");
        nameDialog.setHeaderText("Enter your username:");
        nameDialog.setContentText("Username:");
        nameDialog.showAndWait().ifPresent(name -> username = name);

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Username required. Exiting...");
            Platform.exit();
            return;
        }

        // --- Layout ---
        messageBox = new VBox(10);
        messageBox.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(messageBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #E5DDD5;");

        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setFont(Font.font(14));

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-weight: bold;");
        sendButton.setOnAction(e -> sendMessage());

        inputField.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(inputField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(inputBox);
        root.setStyle("-fx-background-color: #ECE5DD;");

        Scene scene = new Scene(root, 420, 500);
        primaryStage.setTitle("Chat - " + username);
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Listen for messages
            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        String finalMsg = msg;
                        Platform.runLater(() -> addMessage(finalMsg, false)); // received
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> addSystemMessage("Disconnected from server."));
                }
            });
            listener.setDaemon(true);
            listener.start();

        } catch (IOException e) {
            showError("Unable to connect to server!");
        }
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty() && out != null) {
            String formattedMsg = username + ": " + msg;
            out.println(formattedMsg);
            addMessage(formattedMsg, true); // show sent message locally
            inputField.clear();
        }
    }

    private void addMessage(String message, boolean isOwn) {
        HBox msgContainer = new HBox();
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setPadding(new Insets(8, 12, 8, 12));
        msgLabel.setFont(Font.font(14));
        msgLabel.setMaxWidth(260);

        if (isOwn) {
            msgContainer.setAlignment(Pos.CENTER_RIGHT);
            msgLabel.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10;");
        } else {
            msgContainer.setAlignment(Pos.CENTER_LEFT);
            msgLabel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        }

        msgContainer.getChildren().add(msgLabel);
        messageBox.getChildren().add(msgContainer);
    }

    private void addSystemMessage(String text) {
        Label sysMsg = new Label(text);
        sysMsg.setTextFill(Color.GRAY);
        sysMsg.setFont(Font.font(12));
        messageBox.getChildren().add(new HBox(sysMsg));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (socket != null)
            socket.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
