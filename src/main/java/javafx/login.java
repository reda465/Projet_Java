package javafx;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
public class login extends Application {
    @Override
    public void start(Stage stage) {
        stage.setScene(creerScene(stage));
        stage.setTitle("WhatsApp - Login");
        stage.show();
    }
    public static Scene creerScene(Stage stage) {
        String fs = fieldStyle();
        String bs = btnStyle();
        //  LOGO
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

        //  USERNAME
        TextField username = new TextField();
        username.setPromptText("Nom d'utilisateur");
        username.setStyle(fs);
        focusStyle(username, fs);

        //  PASSWORD
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        password.setStyle(fs);
        focusStyle(password, fs);
        //  oublier
        Hyperlink oublier = new Hyperlink("Mot de passe oublié ?");
        oublier.setTextFill(Color.web("#25D366"));
        oublier.setStyle("-fx-border-color: transparent; -fx-font-size: 12px;");
        HBox forgotBox = new HBox(oublier);
        forgotBox.setAlignment(Pos.CENTER_RIGHT);

        //  BOUTON LOGIN
        Button loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(bs);

        loginBtn.setOnMouseEntered(e ->
                loginBtn.setStyle(bs.replace("#25D366", "#128C7E"))
        );
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(bs));

        //  MESSAGE
        Label message = new Label();
        message.setFont(Font.font("Arial", 13));
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);

        //  LOGIQUE LOGIN
        loginBtn.setOnAction(e -> {
            String u = username.getText().trim();
            String p = password.getText();

            if (u.isEmpty() || p.isEmpty()) {
                message.setTextFill(Color.RED);
                message.setText("Veuillez remplir tous les champs");
                return;
            }
            if (u.equals("admin") && p.equals("1234")) {
                message.setTextFill(Color.web("#25D366"));
                message.setText("Connexion réussie !");
            } else {
                message.setTextFill(Color.RED);
                message.setText("Identifiants incorrects");}});
        //  SIGNUP
        Label noAccount = new Label("Pas de compte ?");
        noAccount.setFont(Font.font("Arial", 12));
        noAccount.setTextFill(Color.web("#4CAF50"));

        Hyperlink signupLink = new Hyperlink("S'inscrire");
        signupLink.setTextFill(Color.web("#25D366"));

        signupLink.setOnAction(e ->
                stage.setScene(signup.creerScene(stage)) //  lier avec sigup
        );

        HBox signupBox = new HBox(5, noAccount, signupLink);
        signupBox.setAlignment(Pos.CENTER);

        //  CARD
        VBox card = new VBox(10, header, username, password, forgotBox, loginBtn, message, signupBox);
        card.setPadding(new Insets(30));
        card.setPrefWidth(320);
        card.setStyle(cardStyle());
        // BACKGROUND
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E);");
        root.setPadding(new Insets(40));

        return new Scene(root, 420, 600);
    }
    // STYLE
    static String fieldStyle() {
        return "-fx-background-color:#ECFFF5;" +
                "-fx-border-color:#A5E6C3;" +
                "-fx-border-radius:11px;" +
                "-fx-background-radius:11px;" +
                "-fx-padding:10px;";
    }
    // STYLE BUTTON
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
    // FOCUS EFFECT
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
