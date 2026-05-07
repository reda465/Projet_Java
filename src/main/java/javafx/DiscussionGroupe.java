package javafx;

import client.ClientHandlerAuth;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DiscussionGroupe {
    private static final Map<Integer, DiscussionGroupe> discussionsOuvertes = new HashMap<>();
    private final Groupe groupe;
    private final VBox messagesBox = new VBox(8);

    public DiscussionGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public void ouvrir(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Groupe - " + groupe.getNomGroupe());
        discussionsOuvertes.put(groupe.getIdGroupe(), this);
        stage.setOnCloseRequest(e -> discussionsOuvertes.remove(groupe.getIdGroupe()));

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
        MenuItem ajouterItem = new MenuItem("Ajouter membre");
        MenuItem retirerItem = new MenuItem("Retirer membre");
        MenuItem renommerItem = new MenuItem("Renommer");
        MenuItem quitterItem = new MenuItem("Quitter");
        MenuItem supprimerItem = new MenuItem("Supprimer");
        menu.getItems().addAll(ajouterItem, retirerItem, renommerItem, quitterItem, supprimerItem);

        ajouterItem.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Ajouter membre");
            d.setHeaderText(null);
            d.setContentText("Numéro du membre :");
            Optional<String> r = d.showAndWait();
            r.ifPresent(num -> {
                String numero = num.trim();
                if (!numero.isEmpty()) ClientHandlerAuth.getInstance().ajouterMembreAuGroupe(groupe.getIdGroupe(), numero);
            });
        });
        retirerItem.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Retirer membre");
            d.setHeaderText(null);
            d.setContentText("Numéro du membre :");
            d.showAndWait().ifPresent(num -> {
                String numero = num.trim();
                if (!numero.isEmpty()) ClientHandlerAuth.getInstance().retirerMembreDuGroupe(groupe.getIdGroupe(), numero);
            });
        });
        renommerItem.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog(groupe.getNomGroupe());
            d.setTitle("Renommer groupe");
            d.setHeaderText(null);
            d.setContentText("Nouveau nom :");
            d.showAndWait().ifPresent(nom -> {
                String nv = nom.trim();
                if (!nv.isEmpty()) {
                    ClientHandlerAuth.getInstance().modifierNomGroupe(groupe.getIdGroupe(), nv);
                    groupe.setNomGroupe(nv);
                    title.setText(groupe.getNomGroupe() + " (" + (groupe.getNumerosMembres() != null ? groupe.getNumerosMembres().size() : 0) + " membres)");
                }
            });
        });
        quitterItem.setOnAction(e -> {
            ClientHandlerAuth.getInstance().quitterGroupe(groupe.getIdGroupe());
            stage.close();
        });
        supprimerItem.setOnAction(e -> {
            ClientHandlerAuth.getInstance().supprimerGroupe(groupe.getIdGroupe());
            stage.close();
        });
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
            MessageGroupe msg = new MessageGroupe();
            msg.setNomExpediteur(ClientHandlerAuth.getInstance().getUtilisateurConnecte() != null
                    ? ClientHandlerAuth.getInstance().getUtilisateurConnecte().getNomComplet()
                    : "Moi");
            msg.setContenu(txt.trim());
            afficherMessage(msg);
            field.clear();
        };
        send.setOnAction(e -> action.run());
        field.setOnAction(e -> action.run());
        input.getChildren().addAll(field, send);
        root.setBottom(input);
        ClientHandlerAuth.getInstance().demanderMessagesGroupe(groupe.getIdGroupe());

        stage.setScene(new Scene(root, 620, 520));
        stage.show();
    }

    public void afficherMessage(MessageGroupe msg) {
        String contenu = msg.getContenu() != null ? msg.getContenu() : "";
        String heure = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        messagesBox.getChildren().add(Messagefx.Messagerecu(msg.getNomExpediteur() + " : " + contenu, heure));
    }

    public static void afficherMessageSiOuvert(MessageGroupe msg) {
        if (msg == null) return;
        DiscussionGroupe d = discussionsOuvertes.get(msg.getIdGroupe());
        if (d != null) d.afficherMessage(msg);
    }
}
