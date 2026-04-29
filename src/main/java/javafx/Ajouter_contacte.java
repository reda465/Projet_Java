package javafx;
import client.ClientHandlerAuth;
import client.ClientReseau;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class Ajouter_contacte {
    public static void show(Stage parentStage, ListView<HBox> convList, ClientHandlerAuth client) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.initOwner(parentStage);
        window.setTitle("Ajouter un contact");
        Circle circle = new Circle(24);
        circle.setFill(Color.web("#25D366"));
        Text w = new Text("👤");
        w.setFont(Font.font("Arial", 18));
        StackPane logo = new StackPane(circle, w);

        Label title = new Label("Nouveau Contact");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#128C7E"));

        VBox header = new VBox(6, logo, title);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));

        // ─── Champs ────────────────────────────────────────────────────────
        TextField nameField = new TextField();
        nameField.setPromptText("Nom du contact");
        nameField.setStyle(fieldStyle());
        focusStyle(nameField, fieldStyle());

        TextField phoneField = new TextField();
        phoneField.setPromptText("Numéro de téléphone");
        phoneField.setStyle(fieldStyle());
        focusStyle(phoneField, fieldStyle());

        // ─── Message feedback ──────────────────────────────────────────────
        Label message = new Label();
        message.setWrapText(true);
        message.setFont(Font.font("Arial", 12));

        // ─── Bouton Ajouter ────────────────────────────────────────────────
        Button addBtn = new Button("Ajouter le contact");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle(btnStyle());
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(btnStyle().replace("#25D366", "#128C7E")));
        addBtn.setOnMouseExited(e  -> addBtn.setStyle(btnStyle()));

        addBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String phone = phoneField.getText().trim();

            // Validation
            if (name.isEmpty() || phone.isEmpty()) {
                message.setTextFill(Color.RED);
                message.setText("⚠ Veuillez remplir tous les champs");
                return;
            }
            if (phone.replaceAll("\\s", "").length() < 8) {
                message.setTextFill(Color.RED);
                message.setText("⚠ Numéro invalide (min 8 chiffres)");
                return;
            }

            // ── 1. Choisir une couleur aléatoire pour l'avatar ──
            String[] colors = {"#25D366", "#128C7E", "#075E54", "#34B7F1"};
            String color = colors[(int)(Math.random() * colors.length)];

            // ── 2. Créer l'item et l'ajouter dans convList de Discussion ──
            HBox item = Discussion.makeConvItem(name, phone, "maintenant", color);
            convList.getItems().add(item);
            //

            message.setTextFill(Color.web("#25D366"));
            message.setText("✓ Contact \"" + name + "\" ajouté !");
            nameField.clear();
            phoneField.clear();
        });

        // ─── Bouton Annuler ────────────────────────────────────────────────
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #A5E6C3;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-text-fill: #128C7E;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8px;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> window.close());
        //box
        VBox layout = new VBox(10,
                header,
                new Label("Nom"),
                nameField,
                new Label("Téléphone"),
                phoneField,
                addBtn,
                cancelBtn,
                message
        );
        layout.setPadding(new Insets(24));
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setStyle(cardStyle());

        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E);");
        root.setPadding(new Insets(20));

        window.setScene(new Scene(root, 320, 400));
        window.show();
    }
    // ─── Styles ────────────────────────────────────────────────────────────
    static String fieldStyle() {
        return "-fx-background-color:#ECFFF5;" +
                "-fx-border-color:#A5E6C3;" +
                "-fx-border-radius:11px;" +
                "-fx-background-radius:11px;" +
                "-fx-padding:10px;";
    }
    static String btnStyle() {
        return "-fx-background-color:#25D366;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;" +
                "-fx-background-radius:12px;" +
                "-fx-padding:10px;" +
                "-fx-cursor:hand;";
    }

    static String cardStyle() {
        return "-fx-background-color:white;" +
                "-fx-background-radius:20px;" +
                "-fx-border-color:#A5E6C3;";
    }
    static void focusStyle(TextField tf, String base) {
        tf.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) tf.setStyle(base.replace("#A5E6C3", "#25D366"));
            else         tf.setStyle(base);
        });
    }
}