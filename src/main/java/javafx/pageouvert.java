package javafx;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
public class pageouvert extends Application {
    @Override
    public void start(Stage stage) {
        //  LOGO WhatsApp
        ImageView logo = new ImageView(
                new Image("https://upload.wikimedia.org/wikipedia/commons/6/6b/WhatsApp.svg"));

        logo.setFitWidth(100);
        logo.setFitHeight(100);
        //  ICONES
        ImageView img1 = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/124/124034.png"));
        ImageView img2 = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/2950/2950651.png"));
        ImageView img3 = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/1077/1077012.png"));
        img1.setFitWidth(40);
        img1.setFitHeight(40);
        img2.setFitWidth(40);
        img2.setFitHeight(40);
        img3.setFitWidth(40);
        img3.setFitHeight(40);
        HBox icons = new HBox(15, img1, img2, img3);
        icons.setAlignment(Pos.CENTER);
        //  TITRE
        Label title = new Label("Bienvenue sur WhatsApp 💬📱");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        //  DESCRIPTION
        Label desc = new Label("Discutez avec vos amis facilement 😊🚀");
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        //  BOUTON START
        Button startBtn = new Button("Commencer 🚀");
        startBtn.setStyle(
                "-fx-background-color:#25D366;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14px;" +
                        "-fx-padding:10px 20px;" +
                        "-fx-background-radius:20px;"
        );
        // ACTION BOUTON
        startBtn.setOnAction(e -> {
            stage.close();

            try {
                login loginPage = new login();
                Stage newStage = new Stage();
                loginPage.start(newStage);
            } catch (Exception ex) {
                ex.printStackTrace();}});
        //
        VBox root = new VBox(20, logo, icons, title, desc, startBtn
        );
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #25D366, #128C7E);");
        // SCENE
        Scene scene = new Scene(root, 400, 600);
        stage.setScene(scene);
        stage.setTitle("WhatsApp Welcome");
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}