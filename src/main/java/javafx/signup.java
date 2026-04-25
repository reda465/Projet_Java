package javafx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
public class signup {
    public static Scene creerScene(Stage stage) {
        // Styles venant de login
        String fs = login.fieldStyle();
        String bs = login.btnStyle();
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
        TextField nom = new TextField();
        nom.setPromptText("Nom");
        nom.setStyle(fs);
        TextField prenom = new TextField();
        prenom.setPromptText("Prénom");
        prenom.setStyle(fs);

        TextField tel = new TextField();
        tel.setPromptText("+212...");
        tel.setStyle(fs);
        PasswordField pass1 = new PasswordField();
        pass1.setPromptText("Mot de passe");
        pass1.setStyle(fs);
        PasswordField pass2 = new PasswordField();
        pass2.setPromptText("Confirmer mot de passe");
        pass2.setStyle(fs);
        // Message
        Label message = new Label();
        message.setFont(Font.font("Arial", 12));
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);
        // Bouton
        Button btn = new Button("S'inscrire");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(bs);
        // Hover (même couleur login)
        btn.setOnMouseEntered(e ->
                btn.setStyle(bs.replace("#b91d73", "#25D366")));

        btn.setOnMouseExited(e -> btn.setStyle(bs));
        // Action
        btn.setOnAction(e -> {
            String n = nom.getText().trim();
            String p = prenom.getText().trim();
            String t = tel.getText().trim();
            String pw = pass1.getText();
            String pw2 = pass2.getText();
            if (n.isEmpty() || p.isEmpty() || t.isEmpty() || pw.isEmpty() || pw2.isEmpty()) {
                message.setTextFill(Color.web("#25D366"));
                message.setText("Tous les champs sont obligatoires");
                return;
            }
            if (!pw.equals(pw2)) {
                message.setTextFill(Color.web("#25D366"));
                message.setText("Les mots de passe ne correspondent pas");
                return;
            }
            if (t.replaceAll("\\s", "").length() < 8) {
                message.setTextFill(Color.web("#b91d73"));
                message.setText("Numéro invalide");
                return;
            }
            message.setTextFill(Color.web("#f953c6"));
            message.setText("Compte créé pour " + p + " " + n);});
        // Link login
        Hyperlink loginLink = new Hyperlink("Se connecter");
        loginLink.setTextFill(Color.web("#25D366"));
        loginLink.setOnAction(e ->
                stage.setScene(login.creerScene(stage))
        );
        HBox linkBox = new HBox(5, new Label("Déjà un compte ?"), loginLink);
        linkBox.setAlignment(Pos.CENTER);
        // Card
        VBox card = new VBox(10, header, nom, prenom, tel, pass1, pass2, btn, message, linkBox);
        card.setPadding(new Insets(30));
        card.setPrefWidth(320);
        card.setStyle(login.cardStyle());
        //
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #25D366);");
        root.setPadding(new Insets(40));
        return new Scene(root, 420, 700);
    }
}