package javafx;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;//VBOX
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.collections.*;
import java.time.LocalTime;//pour recuperer heure actuelle
import java.time.format.DateTimeFormatter;
public class Discussion extends Application {
    @Override
    public void start(Stage stage) {
        stage.setScene(creerScene(stage));
        stage.setTitle("WhatsApp - Discussions");//titre de la fenetre
        stage.show();//pour afficher la fenetre
    }
    public static Scene creerScene(Stage stage) {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(300);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #E9EDEF; -fx-border-width: 0 1 0 0;");
        // Header sidebar
        HBox sideHeader = new HBox(10);
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        sideHeader.setPadding(new Insets(14));
        sideHeader.setStyle("-fx-background-color: #F0F2F5;");

        StackPane myAvatar = makeAvatar("M", "#128C7E");

        Label myName = new Label("Mon Compte");
        myName.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        myName.setTextFill(Color.web("#111B21"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        //  BOUTON "Ajouter un contact" — icône personne+
        Button addContactBtn = new Button("👤+");
        addContactBtn.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;"
        );
        addContactBtn.setTooltip(new Tooltip("Ajouter un contact"));
        addContactBtn.setOnMouseEntered(e -> addContactBtn.setStyle(
                "-fx-background-color: #128C7E;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;"
        ));
        addContactBtn.setOnMouseExited(e -> addContactBtn.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-cursor: hand;"
        ));

        sideHeader.getChildren().addAll(myAvatar, myName, spacer, addContactBtn);

        // Barre de recherche
        TextField search = new TextField();
        search.setPromptText("🔍  Rechercher");
        search.setStyle(
                "-fx-background-color: #F0F2F5;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 8px 12px;" +
                        "-fx-font-size: 13px;"
        );
        HBox searchBox = new HBox(search);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 10, 8, 10));
        searchBox.setStyle("-fx-background-color: #F0F2F5;");

        // Liste des conversations
        ListView<HBox> convList = new ListView<>();
        convList.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-border-color: transparent;");
        VBox.setVgrow(convList, Priority.ALWAYS);

        // Données initiales
        String[][] convData = {
                {"Samira",  "occupe",              "Hier",      "1", "#075E54"},
                {"Karim",   "À tout à l'heure !",  "09:30",     "0", "#128C7E"},
                {"Yasmine", "Photo envoyée 📷",    "Lun",       "3", "#25D366"},
        };
        ObservableList<HBox> items = FXCollections.observableArrayList();
        for (String[] c : convData) {
            items.add(makeConvItem(c[0], c[1], c[2], c[3], c[4]));
        }
        convList.setItems(items);

        //  Lier le bouton à la popup ajouter_contacte
        addContactBtn.setOnAction(e -> Ajouter_contacte.show(stage, convList));

        sidebar.getChildren().addAll(sideHeader, searchBox, convList);

        // ─── CHAT PANEL ─────────────────────────────────────────────────────
        VBox chatPanel = new VBox(0);
        VBox.setVgrow(chatPanel, Priority.ALWAYS);
        chatPanel.setStyle("-fx-background-color: #EFEAE2;");

        // Header chat
        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(10, 16, 10, 16));
        chatHeader.setStyle("-fx-background-color: #F0F2F5;");

        StackPane chatAvatar = makeAvatar("S", "#25D366");
        VBox chatInfo = new VBox(2);
        Label chatName = new Label("Samira");
        chatName.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        chatName.setTextFill(Color.web("#111B21"));
        Label chatStatus = new Label("en ligne");
        chatStatus.setFont(Font.font("Arial", 12));
        chatStatus.setTextFill(Color.web("#25D366"));
        chatInfo.getChildren().addAll(chatName, chatStatus);

        Region chatSpacer = new Region();
        HBox.setHgrow(chatSpacer, Priority.ALWAYS);

        chatHeader.getChildren().addAll(chatAvatar, chatInfo, chatSpacer);

        // Zone messages
        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(15));

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        //  Utilisation de la classe message pour les bulles
        String[][] msgs = {
                {"recv", "Salut !",          "09:42"},
                {"send", "Oui je suis là",   "09:43"},
                {"recv", "Projet ?",         "09:44"},
        };
        for (String[] m : msgs) {
            if (m[0].equals("send")) {
                messagesBox.getChildren().add(Message.Messageenvoyer(m[1], m[2]));
            } else {
                messagesBox.getChildren().add(Message.Messageereçu(m[1], m[2]));
            }
        }

        // Barre de saisie
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setStyle("-fx-background-color: #F0F2F5;");

        TextField msgField = new TextField();
        msgField.setPromptText("Tapez un message");
        msgField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 21px;" +
                        "-fx-background-radius: 21px;" +
                        "-fx-padding: 10px 14px;" +
                        "-fx-font-size: 14px;"
        );
        HBox.setHgrow(msgField, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 40px;" +
                        "-fx-min-height: 40px;" +
                        "-fx-cursor: hand;"
        );
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(
                "-fx-background-color: #128C7E;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 40px;" +
                        "-fx-min-height: 40px;" +
                        "-fx-cursor: hand;"
        ));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 40px;" +
                        "-fx-min-height: 40px;" +
                        "-fx-cursor: hand;"
        ));

        //  Envoi du message avec la classe message
        Runnable sendAction = () -> {
            String text = msgField.getText().trim();
            if (!text.isEmpty()) {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                messagesBox.getChildren().add(Message.Messageenvoyer(text, time));
                msgField.clear();
                scrollPane.setVvalue(1.0);
            }
        };
        sendBtn.setOnAction(e -> sendAction.run());
        msgField.setOnAction(e -> sendAction.run());

        inputBar.getChildren().addAll(msgField, sendBtn);
        chatPanel.getChildren().addAll(chatHeader, scrollPane, inputBar);

        // ─── ROOT ────────────────────────────────────────────────────────────
        HBox root = new HBox(0, sidebar, chatPanel);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);

        return new Scene(root, 900, 620);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    static StackPane makeAvatar(String letter, String color) {
        Circle c = new Circle(20);
        c.setFill(Color.web(color));
        Text t = new Text(letter);
        t.setFill(Color.WHITE);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        return new StackPane(c, t);
    }

    static HBox makeConvItem(String name, String last, String time, String unread, String color) {
        StackPane avatar = makeAvatar(String.valueOf(name.charAt(0)), color);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#111B21"));

        Label lastLabel = new Label(last);
        lastLabel.setFont(Font.font("Arial", 12));
        lastLabel.setTextFill(Color.web("#667781"));

        VBox info = new VBox(3, nameLabel, lastLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(10, avatar, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #F0F2F5; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand;"));

        if (!unread.equals("0")) {
            Label badge = new Label(unread);
            badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            badge.setTextFill(Color.WHITE);
            badge.setStyle("-fx-background-color: #25D366; -fx-background-radius: 50%; -fx-padding: 2px 6px;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            // Remplace info spacer par un layout avec badge
            VBox infoWithBadge = new VBox(3, nameLabel, lastLabel);
            HBox rowFull = new HBox(10, avatar, infoWithBadge, sp, badge);
            rowFull.setAlignment(Pos.CENTER_LEFT);
            rowFull.setPadding(new Insets(10, 14, 10, 14));
            rowFull.setStyle("-fx-cursor: hand;");
            rowFull.setOnMouseEntered(e -> rowFull.setStyle("-fx-background-color: #F0F2F5; -fx-cursor: hand;"));
            rowFull.setOnMouseExited(e -> rowFull.setStyle("-fx-cursor: hand;"));
            return rowFull;
        }
        return row;
    }
    public static void main(String[] args) {
        launch(args);
    }
}