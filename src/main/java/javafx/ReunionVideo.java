package javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ReunionVideo {
    private final GridPane grille = new GridPane();

    public void ouvrir(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Réunion vidéo");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F0F2F5;");

        grille.setHgap(8);
        grille.setVgap(8);
        grille.setPadding(new Insets(10));
        for (int i = 0; i < 4; i++) {
            ImageView view = new ImageView();
            view.setFitWidth(240);
            view.setFitHeight(160);
            view.setStyle("-fx-background-color:#000000;");
            grille.add(view, i % 2, i / 2);
        }
        root.setCenter(grille);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10));
        actions.setAlignment(Pos.CENTER);
        Button cam = new Button("Activer caméra");
        Button mic = new Button("Couper micro");
        Button quit = new Button("Quitter");
        quit.setOnAction(e -> stage.close());
        actions.getChildren().addAll(cam, mic, quit);
        root.setBottom(actions);

        stage.setScene(new Scene(root, 520, 420));
        stage.show();
    }
}
