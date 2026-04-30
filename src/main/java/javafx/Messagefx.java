
package javafx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
}