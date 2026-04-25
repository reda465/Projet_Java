package javafx;
import javafx.geometry.Insets;//padding
import javafx.geometry.Pos;//pour choisir la position
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
public class Message {
    //  MESSAGE envoyer
    public static HBox Messageenvoyer(String text, String time) {
        Label msg = new Label(text);//un texte qui affiche le message
        msg.setWrapText(true);//si le message est long
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);//couleur de texte
        Label tm = new Label(time);//pour l'heure
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.web("#53BDEB"));//bleu clair
        VBox mesgh = new VBox(4, msg, tm);
        mesgh.setPadding(new Insets(8));//padding
        mesgh.setStyle(
                "-fx-background-color: #D9FDD3;" +
                        "-fx-background-radius: 12 2 12 12;");
        HBox lignemsg= new HBox(mesgh);
        lignemsg.setAlignment(Pos.CENTER_RIGHT);
        lignemsg.setPadding(new Insets(2, 10, 2, 10));//haut ,droit,bas,gauche

        return lignemsg;//retoune message pret a afficher
    }
    public static HBox Messageereçu(String text, String time) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);
        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.GRAY);
        VBox mesgh = new VBox(4, msg, tm);
        mesgh.setPadding(new Insets(8));
        mesgh.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 2 12 12 12;");
        HBox lignemsg= new HBox();
        lignemsg.setAlignment(Pos.CENTER_LEFT);
        lignemsg.setPadding(new Insets(2, 10, 2, 10));
        return lignemsg;
    }
}
