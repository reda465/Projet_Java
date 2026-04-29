package javafx;
import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Utilisateur;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.collections.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
public class Discussion implements EcouteurClient {
    // utilisateur actuellement connecter
    private Utilisateur utilisateurConnecte;
    private String contactActif = null;//status

    // composantes de l'interface
    private VBox      messagesBox;
    private ScrollPane scrollPane;
    private Label     chatStatus;
    private Label     chatName;
    private StackPane chatAvatar;
    private TextField msgField;
    private Button    sendBtn;
    private Stage     primaryStage;
    ListView<HBox>    convList;//liste des contace

    public Discussion(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }
    // ── Construction de la scène
    public Scene creerScene(Stage stage) {
        this.primaryStage = stage;
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(300);
        sidebar.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #dadada;" +
                        "-fx-border-width: 0 1 0 0;");

        // -- nom de l'utilisateur
        HBox sideHeader = new HBox(10);
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        sideHeader.setPadding(new Insets(10,16,10,16));
        sideHeader.setStyle("-fx-background-color: #25D366;");

        // Avatar utilisateur connecté
        String initiale = utilisateurConnecte != null ?
                String.valueOf(utilisateurConnecte.getNomComplet().charAt(0)).toUpperCase() : "?";
        StackPane userAvatar = makeAvatar(initiale, "#075E54");

        Label userName = new Label(utilisateurConnecte != null ?
                utilisateurConnecte.getNomComplet() : "Utilisateur");
        userName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        userName.setTextFill(Color.WHITE);
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        //on ouvre la fenetre pour ajouter un contacte

        Button addContactBtn = new Button("👤+");
        styleIconBtn(addContactBtn, "#25D366", "#128C7E");
        addContactBtn.setTooltip(new Tooltip("Ajouter un contact"));

        sideHeader.getChildren().addAll(userAvatar, userName, spacerHeader, addContactBtn);
        // Zone de recherche
        TextField search = new TextField();
        search.setPromptText("🔍  Rechercher");
        search.setStyle("-fx-background-color: #f0f0f0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 8px 12px;" +
                        "-fx-font-size: 13px;"
        );
        HBox searchBox = new HBox(search);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 10, 8, 10));
        searchBox.setStyle("-fx-background-color: #ffffff;");

        // -- Liste conversations --
        convList = new ListView<>();
        convList.setStyle(
                "-fx-background-insets: 0;" +
                        "-fx-padding: 0;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(convList, Priority.ALWAYS);
        convList.setItems(FXCollections.observableArrayList());

        Label placeholder = new Label("Aucun contact.\nAppuyez sur 👤+ pour en ajouter.");
        placeholder.setTextAlignment(TextAlignment.CENTER);
        placeholder.setTextFill(Color.web("#8696a0"));
        placeholder.setFont(Font.font("Segoe UI", 13));
        convList.setPlaceholder(placeholder);

        // Bouton ajouter contact
        addContactBtn.setOnAction(e ->
                Ajouter_contacte.show(stage, convList, ClientHandlerAuth.getInstance())
        );

        // Clic sur un contact
        convList.setOnMouseClicked(e -> {
            HBox selected = convList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Object ud = selected.getUserData();
            if (ud == null) return;
            String[] parts = ((String) ud).split(";", 2);
            String numero = parts[0];
            String nom    = parts.length > 1 ? parts[1] : numero;
            ouvrirConversation(numero, nom);
        });

        sidebar.getChildren().addAll(sideHeader, searchBox, convList);

        // ── PANEL CHAT  S samira────────────────────────────────────────────────────────
        VBox chatPanel = new VBox(0);//hadik likathl f jnb
        VBox.setVgrow(chatPanel, Priority.ALWAYS);
        chatPanel.setStyle("-fx-background-color: #e5ddd5;");

        // -
        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(10, 16, 10, 16));
        chatHeader.setStyle("-fx-background-color: #25D366;");

        chatAvatar = makeAvatar("?", "#dfe5e7");

        VBox chatInfo = new VBox(2);
        chatName = new Label("");//le nom de la personne
        chatName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        chatName.setTextFill(Color.web("#ffffff"));

        chatStatus = new Label("");
        chatStatus.setFont(Font.font("Segoe UI", 12));
        chatStatus.setTextFill(Color.web("#ffffff"));

        chatInfo.getChildren().addAll(chatName, chatStatus);

        Region chatSpacer = new Region();
        HBox.setHgrow(chatSpacer, Priority.ALWAYS);

        chatHeader.getChildren().addAll(chatAvatar, chatInfo, chatSpacer);

        // -- Zone des messages --
        messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(15));
        messagesBox.setFillWidth(true);

        afficherAccueil();

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // -- Barre de saisie --
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setStyle("-fx-background-color: #f0f0f0;");

        msgField = new TextField();//pour ecrire un message
        msgField.setPromptText("Tapez un message");
        msgField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 21px;" +
                        "-fx-background-radius: 21px;" +
                        "-fx-padding: 10px 14px;" +
                        "-fx-font-size: 14px;"
        );
        msgField.setDisable(true);
        HBox.setHgrow(msgField, Priority.ALWAYS);

        sendBtn = new Button("➤");//pour envoyer un message
        styleIconBtn(sendBtn, "#25D366", "#128C7E");
        sendBtn.setDisable(true);

        // ── Action d'envoi ────────────────────────────────────────────────────
        Runnable sendAction = () -> {
            String text = msgField.getText().trim();
            if (text.isEmpty() || contactActif == null) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            // Affichage message localement
            messagesBox.getChildren().add(Message.Messageenvoyer(text, time));
            scrollToBottom();
            msgField.clear();

            // Envoi réseau via serveur
            ClientHandlerAuth.getInstance().envoyerMessage(contactActif, text);
        };

        sendBtn.setOnAction(e -> sendAction.run());
        msgField.setOnAction(e -> sendAction.run());

        inputBar.getChildren().addAll(msgField, sendBtn);
        chatPanel.getChildren().addAll(chatHeader, scrollPane, inputBar);

        // quant utlisateur clique sur contacte acif
        HBox root = new HBox(0, sidebar, chatPanel);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);

        return new Scene(root, 900, 620);
    }

    // ── Ouvrir une conversation ───────────────────────────────────────────────
    private void ouvrirConversation(String numero, String nom) {
        contactActif = numero;
        chatName.setText(nom);//en change le nom
        chatStatus.setText("en ligne");//le status
        chatStatus.setTextFill(Color.web("#ffffff"));

        mettreAJourAvatar(chatAvatar, String.valueOf(nom.charAt(0)).toUpperCase(), "#25D366");
        messagesBox.getChildren().clear();
        msgField.setDisable(false);//activer la zone d'eciture
        sendBtn.setDisable(false);
        msgField.requestFocus();
    }

    // ── EcouteurClient ───────────────────────────────────────────────────────

    @Override
    public void connexionReussie(Utilisateur moi) {
        // Déjà connecté via login, mais on met à jour si besoin
        this.utilisateurConnecte = moi;
        Platform.runLater(() ->
                System.out.println("[Discussion] Connecté : " + moi.getNomComplet())
        );
    }

    @Override
    public void inscriptionReussie(String msg) {
        Platform.runLater(() ->
                showAlert(Alert.AlertType.INFORMATION, "Inscription", msg)
        );
    }

    @Override
    public void erreur(String message) {
        Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR, "Erreur", message)
        );
    }
    //affichage d'un message recue
    @Override
    public void messageRecu(String num, String contenu) {
        Platform.runLater(() -> {
            String time = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            messagesBox.getChildren()
                    .add(Message.Messagerecu(contenu, time));
            scrollPane.setVvalue(1.0);

            // Mettre à jour badge si conversation pas active
            if (!num.equals(contactActif)) {
                mettreAJourBadgeNonLu(num);
            }
        });
    }
    @Override
    public void deconnexion() {
        Platform.runLater(() -> {
            chatStatus.setText("hors ligne");
            chatStatus.setTextFill(Color.web("#EA2424"));
            showAlert(Alert.AlertType.INFORMATION, "Déconnexion",
                    "Vous avez été déconnecté du serveur.");
        });
    }

    @Override
    public void appelEntrant(String numero, String type, String ipAppelant, String ip) {

    }
    @Override
    public void appelAccepte(String numero) {
    }
    @Override
    public void appelRefuse() {
    }
    @Override
    public void appelTermine(String numero) {

    }
    @Override
    //le seveur affiche tout les converastion
    public void conversationsRecues(List<Conversation> conversations) {
        Platform.runLater(() -> {
            convList.getItems().clear();//vide
            if (conversations == null || conversations.isEmpty()) return;
            String[] colors = {"#25D366", "#128C7E", "#075E54", "#34B7F1"};
            for (Conversation conv : conversations) {
                String nom     = conv.getNomContact();
                String numero  = conv.getNumeroContact();
                String dernier = conv.getDernierMessage() != null
                        ? conv.getDernierMessage() : "";
                String color   = colors[(int)(Math.random() * colors.length)];

                HBox item = makeConvItem(nom, numero, dernier, color);// avatar ,nom,dernier message
                convList.getItems().add(item);
            }
        });
    }

    @Override
    public void contactAjoute(Contact contact) {
    }
    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }
    // ── Badge messages non lus ────────────────────────────────────────────────
    private void mettreAJourBadgeNonLu(String expediteur) {
        for (HBox item : convList.getItems()) {
            Object ud = item.getUserData();
            if (ud == null) continue;
            String numero = ((String) ud).split(";", 2)[0];
            if (!numero.equals(expediteur)) continue;

            item.getChildren().stream()
                    .filter(n -> n instanceof VBox)
                    .map(n -> (VBox) n)
                    .findFirst()
                    .ifPresent(vbox -> {
                        if (vbox.getChildren().size() >= 2) {
                            Label lbl = (Label) vbox.getChildren().get(1);
                            lbl.setText("• Nouveau message");
                            lbl.setTextFill(Color.web("#25D366"));
                        }
                    });
            break;
        }
    }

    // ── Helpers visuels ──────────────────────────────────────────────────────
    private void afficherAccueil() {
        Label msg = new Label("💬  Sélectionnez un contact pour commencer");
        msg.setFont(Font.font("Segoe UI", 13));
        msg.setTextFill(Color.web("#8696a0"));
        msg.setPadding(new Insets(20));
        VBox wrapper = new VBox(msg);
        wrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        messagesBox.getChildren().add(wrapper);
    }

    private void scrollToBottom() {
        scrollPane.setVvalue(1.0);
    }
    public static HBox makeConvItem(String name, String numero, String last, String color) {
        StackPane avatar = makeAvatar(
                String.valueOf(name.charAt(0)).toUpperCase(), color
        );
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#111b21"));

        Label lastLabel = new Label(last);
        lastLabel.setFont(Font.font("Segoe UI", 12));
        lastLabel.setTextFill(Color.web("#667781"));

        VBox info = new VBox(3, nameLabel, lastLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(10, avatar, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-cursor: hand;");
        row.setUserData(numero + ";" + name);

        row.setOnMouseEntered(e -> {
            if (!row.getStyle().contains("#E8F5E9")) {
                row.setStyle("-fx-background-color: #F1F8E9; -fx-cursor: hand;");
            }
        });

        row.setOnMouseExited(e -> {
            if (!row.getStyle().contains("#E8F5E9")) {
                row.setStyle("-fx-cursor: hand;");
            }
        });

        row.setOnMouseClicked(e -> {
            ListView<HBox> list = (ListView<HBox>) row.getParent().getParent();
            for (HBox item : list.getItems()) {
                item.setStyle("-fx-cursor: hand;");
            }
            row.setStyle("-fx-background-color: #E8F5E9; -fx-cursor: hand;");
        });

        return row;
    }

    public static HBox makeConvItem(String name, String last, String time, String unread, String color) {
        return makeConvItem(name, name, last, color);
    }

    static StackPane makeAvatar(String letter, String color) {
        Circle c = new Circle(20);
        c.setFill(Color.web(color));
        Text t = new Text(letter);
        t.setFill(Color.WHITE);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        return new StackPane(c, t);
    }

    private static void mettreAJourAvatar(StackPane avatar, String letter, String color) {
        avatar.getChildren().forEach(n -> {
            if (n instanceof Circle) ((Circle) n).setFill(Color.web(color));
            if (n instanceof Text)   ((Text)   n).setText(letter);
        });
    }

    private static void styleIconBtn(Button btn, String base, String hover) {
        String s1 = "-fx-background-color:" + base  + ";-fx-text-fill:white;" +
                "-fx-font-size:15px;-fx-background-radius:50%;" +
                "-fx-min-width:38px;-fx-min-height:38px;-fx-cursor:hand;";
        String s2 = "-fx-background-color:" + hover + ";-fx-text-fill:white;" +
                "-fx-font-size:15px;-fx-background-radius:50%;" +
                "-fx-min-width:38px;-fx-min-height:38px;-fx-cursor:hand;";
        btn.setStyle(s1);
        btn.setOnMouseEntered(e -> btn.setStyle(s2));
        btn.setOnMouseExited(e  -> btn.setStyle(s1));
    }
    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}