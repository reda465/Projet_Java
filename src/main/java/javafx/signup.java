package javafx;
import client.ClientHandlerAuth;//coomunication avec le serveur
import client.EcouteurClient;//intrface cllback
import javafx.application.Platform;//permet de modifier l'intercfece a partir de thred secondaire
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;//cop graphique (button label textfied password hyperlink
import javafx.scene.layout.*;//vbox hbox
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import model.Utilisateur;
import static javafx.application.Application.launch;
public class signup implements EcouteurClient {
    private Label messageLabel;
    private Button btn;
    private Stage stage;
    // ===== CRÉER LA SCÈNE =====
    public  Scene creerScene(Stage stage) {
        this.stage = stage;
        String chams = login.fieldStyle();
        String btst = login.btnStyle();
        // Logo
        Circle circle = new Circle(30);
        circle.setFill(Color.web("#25D366"));
        Text w = new Text("W");
        w.setFill(Color.WHITE);
        w.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        StackPane logo = new StackPane(circle, w);
        // Titre
        Label title = new Label("Créer un compte");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#25D366"));
        VBox header = new VBox(6, logo, title);
        header.setAlignment(Pos.CENTER);
        // Champs
        TextField nom = new TextField();//vide
        nom.setPromptText("Nom complet");
        nom.setStyle(chams);

        TextField tel = new TextField();
        tel.setPromptText("Numéro de téléphone ");
        tel.setStyle(chams);

        PasswordField pass1 = new PasswordField();
        pass1.setPromptText("Mot de passe");
        pass1.setStyle(chams);

        PasswordField pass2 = new PasswordField();
        pass2.setPromptText("Confirmer mot de passe");
        pass2.setStyle(chams);
        // Message (champ d'instance)
        messageLabel = new Label();
        messageLabel.setFont(Font.font("Arial", 12));
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);
        // Bouton (champ d'instance)
        btn = new Button("S'inscrire");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(chams);
        btn.setOnMouseEntered(e -> btn.setStyle(chams.replace("#25D366", "#128C7E")));
        btn.setOnMouseExited(e -> btn.setStyle(chams));

        // ===== ACTION BOUTON =====
        btn.setOnAction(e -> {
            String nomComplet = nom.getText().trim();
            String numero = tel.getText().trim();
            String pw = pass1.getText();
            String pw2 = pass2.getText();

            // verification
            if (nomComplet.isEmpty() || numero.isEmpty() || pw.isEmpty() || pw2.isEmpty()) {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Tous les champs sont obligatoires");
                return;
            }

            if (!pw.equals(pw2)) {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Les mots de passe ne correspondent pas");
                return;
            }

            if (numero.length() != 10) {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Le numéro doit faire 10 chiffres");
                return;
            }

            // 2. Désactiver le bouton
            btn.setDisable(true);
            messageLabel.setTextFill(Color.web("#128C7E"));
            messageLabel.setText("Inscription en cours...");

            // 3. Envoyer au serveur
            String resultat = ClientHandlerAuth.getInstance().sInscrire(nomComplet, numero, pw);

            // 4. Erreur locale
            if (!resultat.equals("OK")) {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText(resultat);
                btn.setDisable(false);
            }
        });

        // Lien vers login
        Hyperlink loginLink = new Hyperlink("Se connecter");
        loginLink.setTextFill(Color.web("#25D366"));
        loginLink.setOnAction(e -> stage.setScene(new login().creerScene(stage)));

        HBox linkBox = new HBox(5, new Label("Déjà un compte ?"), loginLink);
        linkBox.setAlignment(Pos.CENTER);

        // Card
        VBox card = new VBox(10, header, nom, tel, pass1, pass2, btn, messageLabel, linkBox);
        card.setPadding(new Insets(30));
        card.setPrefWidth(320);
        card.setStyle(login.cardStyle());

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E);");
        root.setPadding(new Insets(40));

        return new Scene(root, 420, 600);
    }
    // ===== CALLBACKS EcouteurClient =====

    @Override
    public void inscriptionReussie(String msg) {
        Platform.runLater(() -> {
            messageLabel.setTextFill(Color.web("#25D366"));
            messageLabel.setText("Compte créé avec succès ! Connectez-vous.");
            btn.setDisable(false);
            // Rediriger vers login après 1.5 secondes
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> stage.setScene(new login().creerScene(stage)));
            }).start();
        });
    }

    @Override
    public void erreur(String message) {
        Platform.runLater(() -> {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText(message);
            btn.setDisable(false);
        });
    }
    @Override
    public void connexionReussie(Utilisateur moi) { /* pas utilisé ici */ }
    @Override
    public void messageRecu(String contenu) { /* pas utilisé ici */ }
    @Override
    public void deconnexion() {
        Platform.runLater(() -> {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Déconnecté du serveur");
            btn.setDisable(false);
        });
    }

    @Override
    public void appelEntrant(String numero, String type) {

    }

    @Override
    public void appelAccepte(String numero) {

    }

    @Override
    public void appelRefuse(String numero) {

    }

    @Override
    public void appelTermine(String numero) {

    }
}
