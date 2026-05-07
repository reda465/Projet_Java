package javafx;

import client.ClientHandlerAuth;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.Groupe;
import model.MessageGroupe;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DiscussionGroupe {
    private final Groupe groupe;
    private final VBox messagesBox = new VBox(8);

    public DiscussionGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public void ouvrir(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Groupe - " + groupe.getNomGroupe());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #e5ddd5;");

        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#25D366;");
        int nb = groupe.getNumerosMembres() != null ? groupe.getNumerosMembres().size() : 0;
        Label title = new Label(groupe.getNomGroupe() + " (" + nb + " membres)");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", 15));
        MenuButton menu = new MenuButton("⋮");
        menu.getItems().addAll(new MenuItem("Ajouter membre"), new MenuItem("Retirer membre"), new MenuItem("Renommer"), new MenuItem("Quitter"), new MenuItem("Supprimer"));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, menu);
        root.setTop(header);

        ScrollPane scroll = new ScrollPane(messagesBox);
        scroll.setFitToWidth(true);
        messagesBox.setPadding(new Insets(12));
        root.setCenter(scroll);

        HBox input = new HBox(8);
        input.setPadding(new Insets(10));
        TextField field = new TextField();
        field.setPromptText("Tapez un message");
        HBox.setHgrow(field, Priority.ALWAYS);
        Button send = new Button("➤");
        send.setStyle("-fx-background-color:#25D366;-fx-text-fill:white;-fx-background-radius:50%;");
        Runnable action = () -> {
            String txt = field.getText();
            if (txt == null || txt.trim().isEmpty()) return;
            ClientHandlerAuth.getInstance().envoyerMessageGroupe(groupe.getIdGroupe(), txt.trim());
            afficherMessage(new MessageGroupe());
            field.clear();
        };
        send.setOnAction(e -> action.run());
        field.setOnAction(e -> action.run());
        input.getChildren().addAll(field, send);
        root.setBottom(input);

        stage.setScene(new Scene(root, 620, 520));
        stage.show();
    }

    public void afficherMessage(MessageGroupe msg) {
        String contenu = msg.getContenu() != null ? msg.getContenu() : "";
        String heure = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        messagesBox.getChildren().add(Messagefx.Messagerecu(msg.getNomExpediteur() + " : " + contenu, heure));
    }
}
