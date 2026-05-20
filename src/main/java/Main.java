
import Serveur.ServeurMT;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.pageouvert;
public class Main {
    public static void main(String[] args) {
        Thread serveurThread = new Thread(() -> {
            System.out.println(" Démarrage du serveur sur le port 5000...");
            ServeurMT.main(new String[]{});
        });
        serveurThread.setDaemon(true);
        serveurThread.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(" Démarrage de l'interface JavaFX...");
        Application.launch(pageouvert.class, args);
    }
}
