
package javafx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
//je dois voir etat de message mn ba3d
public class Messagefx {
    public static HBox Messageenvoyer(String text, String time) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);
        msg.setStyle("-fx-text-fill: black;");
        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.BLACK);
        tm.setStyle("-fx-text-fill: black;");
        VBox bulle = new VBox(4, msg, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(
                "-fx-background-color: #D9FDD3;" +
                        "-fx-background-radius: 12 2 12 12;"+"-fx-text-fill: black;"//a changer car j'aime pas la couleur hadi pour le coin
        );

        HBox ligne = new HBox(bulle);
        ligne.setAlignment(Pos.CENTER_RIGHT);//on met le message envoyer a droite
        ligne.setPadding(new Insets(2, 10, 2, 60));//top right bottom left
        return ligne;
    }

    public static HBox Messagerecu(String text, String time) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);
        msg.setStyle("-fx-text-fill: black;");

        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.web("#555555"));
        tm.setStyle("-fx-text-fill: #555555;");

        VBox bulle = new VBox(4, msg, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 2 12 12 12;"+"-fx-text-fill: black;"
        );
        HBox ligne = new HBox(bulle);
        ligne.setAlignment(Pos.CENTER_LEFT);//message recu a gauche
        ligne.setPadding(new Insets(2, 60, 2, 10));
        return ligne;
    }

    /** Bulle avec contenu personnalisé (image, PDF, audio…) + sous-titre + heure. */
    public static HBox messageFichier(boolean envoye, javafx.scene.Node zoneContenu, String sousTitre, String time) {
        Label sub = new Label(sousTitre != null ? sousTitre : "");
        sub.setWrapText(true);
        sub.setFont(Font.font("Arial", 12));
        sub.setTextFill(Color.web("#333333"));
        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(envoye ? Color.BLACK : Color.web("#555555"));
        if (zoneContenu != null) {
            VBox.setVgrow(zoneContenu, Priority.NEVER);
        }
        VBox bulle = new VBox(6, zoneContenu != null ? zoneContenu : new Region(), sub, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(envoye
                ? "-fx-background-color: #D9FDD3; -fx-background-radius: 12 2 12 12; -fx-text-fill: black;"
                : "-fx-background-color: white; -fx-background-radius: 2 12 12 12; -fx-text-fill: black;");
        HBox ligne = new HBox(bulle);
        ligne.setAlignment(envoye ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        ligne.setPadding(envoye ? new Insets(2, 10, 2, 60) : new Insets(2, 60, 2, 10));
        return ligne;
    }

    public static ImageView miniImagePreview(double maxW, double maxH) {
        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setFitWidth(maxW);
        iv.setFitHeight(maxH);
        iv.setSmooth(true);
        return iv;
    }

    public static Button boutonTelecharger(String libelle) {
        Button b = new Button(libelle);
        b.setStyle("-fx-background-color:#128C7E; -fx-text-fill:white; -fx-background-radius:6px;");
        return b;
    }

    public static ProgressIndicator petitProgress() {
        ProgressIndicator p = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        p.setPrefSize(28, 28);
        return p;
    }
}