/*package javafx;
import com.mysql.cj.xdevapi.Client;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class Appelaudio {
    // Afficher la fenêtre d'appel sortant
    public static void demarrer(Stage parent, String contactNom, Client client) {
        if (client == null || !client.estConnecte()) {
            System.out.println("Impossible d'appeler : client non connecté");
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel audio — " + contactNom);

        Label icone = new Label("📞");
        icone.setFont(Font.font("Arial", 48));

        Label nom = new Label(contactNom);
        nom.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        nom.setTextFill(Color.web("#111B21"));

        Label statut = new Label("Appel en cours...");
        statut.setFont(Font.font("Arial", 14));
        statut.setTextFill(Color.web("#667781"));

        Button btnRaccrocher = new Button("📵  Raccrocher");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 28px;" +
                        "-fx-cursor: hand;"
        );
        btnRaccrocher.setOnAction(e -> {
            // Notifier le serveur de la fin d'appel
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("AUDIO_END:" + contactNom);
            }
            stage.close();
        });

        VBox root = new VBox(18, icone, nom, statut, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #F0F2F5;");

        // Notifier le serveur du démarrage
        if (client != null && client.estConnecte()) {
            client.envoyerMessage("AUDIO_CALL:" + contactNom);
        }

        stage.setScene(new Scene(root, 320, 300));
        stage.show();
    }

    // Afficher la fenêtre d'appel entrant
    public static void recevoirAppel(Stage parent, String appelantNom, Client client) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel entrant");

        Label icone = new Label("📞");
        icone.setFont(Font.font("Arial", 48));

        Label info = new Label(appelantNom + " vous appelle...");
        info.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        info.setTextFill(Color.web("#111B21"));

        Button btnAccepter = new Button("  Accepter");
        btnAccepter.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
        btnAccepter.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("AUDIO_ACCEPT:" + appelantNom);
            }
            stage.close();
            demarrer(parent, appelantNom, client);
        });

        Button btnRefuser = new Button("  Refuser");
        btnRefuser.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
        btnRefuser.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("AUDIO_REFUSE:" + appelantNom);
            }
            stage.close();
        });

        HBox boutons = new HBox(16, btnAccepter, btnRefuser);
        boutons.setAlignment(Pos.CENTER);

        VBox root = new VBox(18, icone, info, boutons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 340, 280));
        stage.show();
    }
}*/
