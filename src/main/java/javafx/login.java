package javafx;
import client.ClientHandlerAuth;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Utilisateur;

import java.util.List;

public class login extends Application implements EcouteurClient {
    private Label message;
    private Stage stage;
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setScene(creerScene(stage));
        stage.setTitle("WhatsApp - Login");
        stage.show();
        ClientHandlerAuth.getInstance()
                .connecterAuServeur("127.0.0.1", 8080, this);
    }
    public Scene creerScene(Stage stage) {
        String fs = fieldStyle();
        String bs = btnStyle();

        // LOGO
        Circle circle = new Circle(32);
        circle.setFill(Color.web("#25D366"));

        Text w = new Text("W");
        w.setFill(Color.WHITE);
        w.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        StackPane logo = new StackPane(circle, w);

        // TITRE
        Label title = new Label("WhatsApp");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#128C7E"));

        Label sub = new Label("Connectez-vous à votre compte");
        sub.setFont(Font.font("Arial", 12));
        sub.setTextFill(Color.web("#4CAF50"));

        VBox header = new VBox(7, logo, title, sub);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        // USERNAME
        TextField numero = new TextField();
        numero.setPromptText("Nom d'utilisateur");
        numero.setStyle(fs);
        focusStyle(numero, fs);

        // PASSWORD
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        password.setStyle(fs);
        focusStyle(password, fs);

        // BUTTON
        Button loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(bs);

        loginBtn.setOnMouseEntered(e ->
                loginBtn.setStyle(bs.replace("#25D366", "#128C7E"))
        );

        loginBtn.setOnMouseExited(e ->
                loginBtn.setStyle(bs)
        );

        // MESSAGE
        message = new Label();
        message.setFont(Font.font("Arial", 13));
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);

        // LOGIN
        loginBtn.setOnAction(e -> {

            String u = numero.getText().trim();
            String p = password.getText();

            if (u.isEmpty() || p.isEmpty()) {
                message.setTextFill(Color.RED);
                message.setText("Veuillez remplir tous les champs");
                return;
            }

            loginBtn.setDisable(true);
            message.setTextFill(Color.web("#128C7E"));
            message.setText("Connexion en cours...");

            new Thread(() -> {

                String resultat =
                        ClientHandlerAuth.getInstance().seConnecter(u, p);

                Platform.runLater(() -> {
                    if (!resultat.equals("OK")) {
                        message.setTextFill(Color.RED);
                        message.setText(resultat);
                        loginBtn.setDisable(false);
                    }
                });

            }).start();
        });

        // SIGNUP
        Label noAccount = new Label("Pas de compte ?");
        noAccount.setFont(Font.font("Arial", 12));
        noAccount.setTextFill(Color.web("#4CAF50"));

        Hyperlink signupLink = new Hyperlink("S'inscrire");
        signupLink.setTextFill(Color.web("#25D366"));

        signupLink.setOnAction(e -> {
            signup s = new signup();
            stage.setScene(s.creerScene(stage));
        });

        HBox signupBox = new HBox(5, noAccount, signupLink);
        signupBox.setAlignment(Pos.CENTER);

        // CARD
        VBox card = new VBox(
                10,
                header,
                numero,
                password,
                loginBtn,
                message,
                signupBox
        );
        card.setPadding(new Insets(30));
        card.setPrefWidth(320);
        card.setStyle(cardStyle());
        // BACKGROUND
        StackPane root = new StackPane(card);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E);"
        );
        root.setPadding(new Insets(40));
        return new Scene(root, 420, 600);
    }
    // ===== CALLBACKS =====
    @Override
    public void connexionReussie(Utilisateur moi) {
        Platform.runLater(() -> {
            message.setTextFill(Color.web("#25D366"));
            message.setText("Connexion réussie ! Bienvenue " + moi.getNomComplet());
        });
    }

    @Override
    public void inscriptionReussie(String msg) {
        // non utilisé dans login
    }
    @Override
    public void erreur(String msg) {
        Platform.runLater(() -> {
            message.setTextFill(Color.RED);
            message.setText(msg);
        });
    }
    @Override
    public void messageRecu(String num, String contenu) {
        // non utilisé dans login
    }

    @Override
    public void conversationsRecues(List<Conversation> conversations) {

    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    @Override
    public void deconnexion() {
        Platform.runLater(() -> {
            message.setTextFill(Color.RED);
            message.setText("Déconnecté du serveur");
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

    // ===== STYLES =====
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
                "-fx-padding:10px;";
    }

    static String cardStyle() {
        return "-fx-background-color:white;" +
                "-fx-background-radius:20px;" +
                "-fx-border-color:#A5E6C3;" +
                "-fx-effect:dropshadow(gaussian, rgba(37,211,102,0.3), 25, 0, 0, 8);";
    }

    static void focusStyle(TextField tf, String base) {
        tf.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                tf.setStyle(base.replace("#A5E6C3", "#25D366"));
            } else {
                tf.setStyle(base);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}