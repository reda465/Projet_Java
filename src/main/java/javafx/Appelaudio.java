package javafx;
import client.ClientHandlerAuth;
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
    // ── Appel audio sortant ───────────────────────────────────────────────────
    public static void demarrer(Stage parent, String contactNom,
                                String numeroContact, int idConversation) {

        if (!ClientHandlerAuth.getInstance().isConnecteAuServeur()) {
            System.out.println("Impossible d'appeler : client non connecté");
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel audio — " + contactNom);

        Label icone = new Label("📞");
        icone.setFont(Font.font("Segoe UI", 48));

        Label nom = new Label(contactNom);
        nom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        nom.setTextFill(Color.web("#111B21"));

        Label statut = new Label("En attente de réponse...");
        statut.setFont(Font.font("Segoe UI", 14));
        statut.setTextFill(Color.web("#667781"));

        /*Label statut = new Label("Appel en cours...");
        statut.setFont(Font.font("Segoe UI", 14));
        statut.setTextFill(Color.web("#667781"));*/

        Button btnRaccrocher = new Button("📵  Raccrocher");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 28px;" +
                        "-fx-cursor: hand;"
        );
        //man3rf
        btnRaccrocher.setOnAction(e -> {
            ClientHandlerAuth.getInstance().raccrocher();
            stage.close();
        });
        VBox root = new VBox(18, icone, nom, statut, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #F0F2F5;");
        // Notifier le serveur du démarrage
        ClientHandlerAuth.getInstance().appeler(
                numeroContact, idConversation, "AUDIO"
        );

        stage.setScene(new Scene(root, 320, 300));
        stage.show();
    }

}