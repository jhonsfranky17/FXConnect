import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        // Login dialog
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Chat Login");
        nameDialog.setHeaderText("Enter your username:");
        nameDialog.setContentText("Username:");
        nameDialog.showAndWait().ifPresent(name -> username = name);

        if (username == null || username.trim().isEmpty()) {
            Platform.exit();
            return;
        }

        // Message area
        messageBox = new VBox(10);
        messageBox.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(messageBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: transparent;");

        // Input field
        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setFont(Font.font(14));
        inputField.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-border-color: transparent;
                -fx-background-radius: 20;
                -fx-padding: 8 15 8 15;
                """);

        // Send button
        Button sendButton = new Button("Send");
        sendButton.setFont(Font.font(14));
        sendButton.setStyle("""
                -fx-background-color: linear-gradient(to right, #FF9800, #F57C00);
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 20;
                -fx-cursor: hand;
                """);
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Header bar
        Label userLabel = new Label(username);
        userLabel.setFont(Font.font(16));
        userLabel.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("● Online");
        status.setTextFill(Color.LIGHTGREEN);

        HBox header = new HBox(10, userLabel, spacer, status);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: linear-gradient(to right, #F57C00, #E65100);");

        // Patterned wallpaper background (orange subtle pattern)
        BackgroundImage wallpaper = new BackgroundImage(
                new javafx.scene.image.Image("https://www.transparenttextures.com/patterns/asfalt-dark.png",
                        600, 600, true, true),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scrollPane);
        root.setBottom(inputBox);
        root.setBackground(new Background(wallpaper));

        Scene scene = new Scene(root, 430, 550);
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

            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        String finalMsg = msg;
                        Platform.runLater(() -> addMessage(finalMsg, false));
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
            addMessage(formattedMsg, true);
            inputField.clear();
        }
    }

    private void addMessage(String message, boolean isOwn) {
        HBox msgContainer = new HBox();
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setPadding(new Insets(10, 15, 10, 15));
        msgLabel.setFont(Font.font(14));
        msgLabel.setMaxWidth(260);
        msgLabel.setEffect(new DropShadow(3, Color.gray(0, 0.2)));

        if (isOwn) {
            msgContainer.setAlignment(Pos.CENTER_RIGHT);
            msgLabel.setStyle("""
                    -fx-background-color: #FFE0B2;
                    -fx-background-radius: 15 15 0 15;
                    -fx-text-fill: #000000;
                    """);
        } else {
            msgContainer.setAlignment(Pos.CENTER_LEFT);
            msgLabel.setStyle("""
                    -fx-background-color: #FFFFFF;
                    -fx-background-radius: 15 15 15 0;
                    -fx-text-fill: #000000;
                    """);
        }

        msgContainer.getChildren().add(msgLabel);
        messageBox.getChildren().add(msgContainer);

        FadeTransition fade = new FadeTransition(Duration.millis(300), msgLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void addSystemMessage(String text) {
        Label sysMsg = new Label(text);
        sysMsg.setTextFill(Color.GRAY);
        sysMsg.setFont(Font.font(12));
        sysMsg.setAlignment(Pos.CENTER);
        HBox box = new HBox(sysMsg);
        box.setAlignment(Pos.CENTER);
        messageBox.getChildren().add(box);
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
