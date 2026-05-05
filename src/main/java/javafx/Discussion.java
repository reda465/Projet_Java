package javafx;
import client.AudioUDP;
import client.ClientHandlerAuth;
import client.EcouteurClient;
import client.VideoUDP;
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
import model.Message;
import java.util.Objects;
public class Discussion implements EcouteurClient {
    private Utilisateur utilisateurConnecte;
    private String contactActif = null;
    private Integer idConversationActive = null;
    private Stage stageAppel = null;
    private AudioUDP audioUDP = null;
    private VideoUDP videoUDP = null;
    private Label statutAppelLabel = null;
    private VBox messagesBox;
    private ScrollPane scrollPane;
    private Label chatStatus;
    private Label chatName;
    private StackPane chatAvatar;
    private TextField msgField;
    private Button sendBtn;
    private Stage primaryStage;
    ListView<HBox> convList;
    private String typeAppelEnCours = null;
    public Discussion(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }
    public Scene creerScene(Stage stage) {
        this.primaryStage = stage;
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(300);
        sidebar.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-border-color: #dadada;" +
                        "-fx-border-width: 0 1 0 0;");

        HBox sideHeader = new HBox(10);
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        sideHeader.setPadding(new Insets(10, 16, 10, 16));
        sideHeader.setStyle("-fx-background-color: #25D366;");

        String initiale = utilisateurConnecte != null ?
                String.valueOf(utilisateurConnecte.getNomComplet().charAt(0)).toUpperCase() : "?";
        StackPane userAvatar = makeAvatar(initiale, "#075E54");

        Label userName = new Label(utilisateurConnecte != null ?
                utilisateurConnecte.getNomComplet() : "Utilisateur");
        userName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        userName.setTextFill(Color.WHITE);
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button addContactBtn = new Button("👤+");
        styleIconBtn(addContactBtn, "#25D366", "#128C7E");
        addContactBtn.setTooltip(new Tooltip("Ajouter un contact"));

        sideHeader.getChildren().addAll(userAvatar, userName, spacerHeader, addContactBtn);

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

        addContactBtn.setOnAction(e ->
                Ajouter_contacte.show(stage, convList, ClientHandlerAuth.getInstance())
        );

        convList.setOnMouseClicked(e -> {
            HBox selected = convList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Object ud = selected.getUserData();
            if (ud == null) return;
            String[] parts = ((String) ud).split(";", 3);
            try {
                int idConv = Integer.parseInt(parts[0]);
                String numero = parts[1];
                String nom = parts[2];
                ouvrirConversation(idConv, numero, nom);
            } catch (NumberFormatException ex) {
                System.out.println(" ID conversation invalide");
            }
        });

        sidebar.getChildren().addAll(sideHeader, searchBox, convList);

        VBox chatPanel = new VBox(0);
        VBox.setVgrow(chatPanel, Priority.ALWAYS);
        chatPanel.setStyle("-fx-background-color: #e5ddd5;");

        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(10, 16, 10, 16));
        chatHeader.setStyle("-fx-background-color: #25D366;");

        chatAvatar = makeAvatar("?", "#dfe5e7");

        VBox chatInfo = new VBox(2);
        chatName = new Label("");
        chatName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        chatName.setTextFill(Color.web("#ffffff"));

        chatStatus = new Label("");
        chatStatus.setFont(Font.font("Segoe UI", 12));
        chatStatus.setTextFill(Color.web("#ffffff"));

        chatInfo.getChildren().addAll(chatName, chatStatus);

        Region chatSpacer = new Region();
        HBox.setHgrow(chatSpacer, Priority.ALWAYS);

        Button btnAppelAudio = new Button("📞");
        styleIconBtn(btnAppelAudio, "#25D366", "#128C7E");
        btnAppelAudio.setTooltip(new Tooltip("Appel audio"));
        btnAppelAudio.setOnAction(e -> {
            if (numeroContactUtilisable(contactActif)) {
                demarrerAppelAudio(contactActif, chatName.getText());
            } else {
                showAlert(Alert.AlertType.WARNING,
                        "Aucun contact", "Sélectionnez un contact d'abord.");
            }
        });
        //BOUTON DE L'appel video
        Button btnAppelVideo = new Button("📹");
        styleIconBtn(btnAppelVideo, "#25D366", "#128C7E");
        btnAppelVideo.setTooltip(new Tooltip("Appel vidéo"));
        btnAppelVideo.setOnAction(e -> {
            if (numeroContactUtilisable(contactActif)) {
                demarrerAppelVideo(contactActif, chatName.getText());
            } else {
                showAlert(Alert.AlertType.WARNING,
                        "Aucun contact", "Sélectionnez un contact d'abord.");
            }
        });

        chatHeader.getChildren().addAll(chatAvatar, chatInfo, chatSpacer, btnAppelAudio,btnAppelVideo);

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

        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setStyle("-fx-background-color: #f0f0f0;");

        msgField = new TextField();
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

        sendBtn = new Button("➤");
        styleIconBtn(sendBtn, "#25D366", "#128C7E");
        sendBtn.setDisable(true);

        Runnable sendAction = () -> {
            String text = msgField.getText().trim();
            if (text.isEmpty() || !numeroContactUtilisable(contactActif)) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            messagesBox.getChildren().add(Messagefx.Messageenvoyer(text, time));
            scrollToBottom();
            msgField.clear();

            ClientHandlerAuth.getInstance().envoyerMessage(contactActif, text);
        };

        sendBtn.setOnAction(e -> sendAction.run());
        msgField.setOnAction(e -> sendAction.run());

        inputBar.getChildren().addAll(msgField, sendBtn);
        chatPanel.getChildren().addAll(chatHeader, scrollPane, inputBar);

        HBox root = new HBox(0, sidebar, chatPanel);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);

        return new Scene(root, 900, 620);
    }

    private void ouvrirConversation(int idconversation, String numero, String nom) {
        this.idConversationActive = idconversation;
        contactActif = normaliserNumeroContact(numero);
        chatName.setText(nom);
        chatStatus.setText("en ligne");
        chatStatus.setTextFill(Color.web("#ffffff"));

        mettreAJourAvatar(chatAvatar, String.valueOf(nom.charAt(0)).toUpperCase(), "#25D366");
        messagesBox.getChildren().clear();
        msgField.setDisable(false);
        majEtatBoutonEnvoi();
        msgField.requestFocus();
        if (idconversation != -1) {
            ClientHandlerAuth.getInstance().demanderMessages(idconversation);
        }
    }

    @Override
    public void connexionReussie(Utilisateur moi) {
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

    @Override
    public void messageRecu(String num, String contenu) {
        Platform.runLater(() -> {
            String time = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            messagesBox.getChildren()
                    .add(Messagefx.Messagerecu(contenu, time));
            scrollPane.setVvalue(1.0);

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

    // ── SEULS CHANGEMENTS : les 4 méthodes d'appel ───────────────────────────

   /* @Override
    public void appelEntrant(String numero, String type, String ipAppelant, String ip) {
        Platform.runLater(() -> {
            String nom = trouverNomContact(numero);
            afficherFenetreAppel(nom, false, numero, ipAppelant);
        });
    }*/
    @Override
    public void appelEntrant(String numero, String type, String ipAppelant, String ip) {
        Platform.runLater(() -> {
            String nom = trouverNomContact(numero);

            if ("VIDEO".equalsIgnoreCase(type)) {
                // ← AJOUT : Appel vidéo entrant → déléguer à Appelvideo
                Appelvideo.recevoirAppel(primaryStage, nom, numero, ipAppelant);
            } else {
                // Appel audio entrant (existant)
                afficherFenetreAppel(nom, false, numero, ipAppelant);
            }
        });
    }

    /*@Override
    public void appelAccepte(String numero, String ip) {
        Platform.runLater(() -> {
            // Mettre à jour le statut affiché dans la fenêtre d'appel
            if (statutAppelLabel != null) {
                statutAppelLabel.setText("En communication...");
            }

            if (ip != null && !ip.isBlank()) {
                audioUDP = new AudioUDP();
                audioUDP.demarrer(ip, 6000, 6001);
                System.out.println("[Audio] Démarré côté appelant → " + ip);
            } else {
                System.out.println("[Audio] IP distante non reçue, audio non démarré.");
                showAlert(Alert.AlertType.ERROR, "Erreur audio",
                        "Impossible de démarrer l'audio : IP distante manquante.");
            }
        });
    }*/
    //HADCHI
    @Override
    public void appelAccepte(String numero, String ip) {
        Platform.runLater(() -> {
            if (statutAppelLabel != null) {
                statutAppelLabel.setText("En communication...");
            }

            if (ip != null && !ip.isBlank()) {
                if ("VIDEO".equalsIgnoreCase(typeAppelEnCours)) {
                    // ← AJOUT : Démarrer la vidéo via Appelvideo
                    Appelvideo.demarrer(primaryStage, chatName.getText(), contactActif,
                            idConversationActive != null ? idConversationActive : -1, ip);

                    // Fermer la fenêtre d'appel audio si elle est ouverte
                    if (stageAppel != null) {
                        stageAppel.close();
                        stageAppel = null;
                    }
                } else {
                    // Audio existant
                    audioUDP = new AudioUDP();
                    audioUDP.demarrer(ip, 6000, 6001);
                    System.out.println("[Audio] Démarré côté appelant → " + ip);
                }
            } else {
                System.out.println("[Audio/Video] IP distante non reçue, média non démarré.");
                showAlert(Alert.AlertType.ERROR, "Erreur média",
                        "Impossible de démarrer : IP distante manquante.");
            }
        });
    }
    @Override
    public void appelRefuse() {
        Platform.runLater(() -> {
            if (stageAppel != null) { stageAppel.close(); stageAppel = null; }
            showAlert(Alert.AlertType.INFORMATION,
                    "Appel refusé", "Le contact a refusé l'appel.");
        });
    }
    @Override
    public void appelTermine(String numero) {
        Platform.runLater(() -> {
            if (audioUDP != null) { audioUDP.arreter(); audioUDP = null; }
            if (videoUDP != null) { videoUDP.arreter(); videoUDP = null; }
            if (stageAppel != null) { stageAppel.close(); stageAppel = null; }
            typeAppelEnCours = null;
            showAlert(Alert.AlertType.INFORMATION,
                    "Appel terminé", "L'appel est terminé.");
        });
    }
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void conversationsRecues(List<Conversation> conversations) {
        Platform.runLater(() -> {
            convList.getItems().clear();
            if (conversations == null || conversations.isEmpty()) return;
            String[] colors = {"#25D366", "#128C7E", "#075E54", "#34B7F1"};
            for (Conversation conv : conversations) {
                String nom = conv.getNomContact();
                String numero = conv.getNumeroContact();
                String dernier = conv.getDernierMessage() != null
                        ? conv.getDernierMessage() : "";
                String color = colors[(int) (Math.random() * colors.length)];
                HBox item = makeConvItem(nom, numero, dernier, color,
                        conv.getIdConversation(), conv.getMessagesNonLus());
                convList.getItems().add(item);
            }
        });
    }

    @Override
    public void messagesRecus(List<Message> messages) {
        Platform.runLater(() -> {
            if (messages == null || messages.isEmpty()) {
                Label emptyLabel = new Label("Aucun message. Commencez la conversation !");
                emptyLabel.setTextFill(Color.web("#8696a0"));
                emptyLabel.setFont(Font.font("Segoe UI", 13));
                messagesBox.getChildren().add(emptyLabel);
                majEtatBoutonEnvoi();
                return;
            }

            for (Message msg : messages) {
                String time = msg.getDateEnvoi() != null
                        ? msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"))
                        : "";
                HBox messageBubble;
                if (msg.isEstMoi()) {
                    messageBubble = Messagefx.Messageenvoyer(msg.getContenuTexte(), time);
                } else {
                    messageBubble = Messagefx.Messagerecu(msg.getContenuTexte(), time);
                }
                messagesBox.getChildren().add(messageBubble);
            }
            if (!numeroContactUtilisable(contactActif)) {
                String deduit = infererNumeroInterlocuteur(messages);
                if (deduit != null) {
                    contactActif = deduit;
                    chatStatus.setText("en ligne");
                }
            }
            majEtatBoutonEnvoi();
            scrollToBottom();
        });
    }

    @Override
    public void contactAjoute(Contact contact) {}

    @Override
    public void listeContactsRecue(List<Contact> contacts) {}

    private void mettreAJourBadgeNonLu(String expediteur) {
        for (HBox item : convList.getItems()) {
            Object ud = item.getUserData();
            if (ud == null) continue;
            String[] parts = ((String) ud).split(";", 3);
            if (parts.length < 3) continue;
            String numero = parts[1];
            if (!numeroContactUtilisable(numero) || !numero.trim().equals(expediteur)) continue;
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

    private static boolean numeroContactUtilisable(String tel) {
        if (tel == null || tel.isBlank()) return false;
        return !"null".equalsIgnoreCase(tel.trim());
    }

    private static String normaliserNumeroContact(String tel) {
        return numeroContactUtilisable(tel) ? tel.trim() : null;
    }

    private static String numeroPourListe(String numero) {
        String n = normaliserNumeroContact(numero);
        return n != null ? n : "";
    }

    private void majEtatBoutonEnvoi() {
        if (sendBtn == null) return;
        sendBtn.setDisable(!numeroContactUtilisable(contactActif));
    }

    private String infererNumeroInterlocuteur(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return null;
        String moiTel = null;
        if (utilisateurConnecte != null && utilisateurConnecte.getNumeroTelephone() != null) {
            moiTel = utilisateurConnecte.getNumeroTelephone().trim();
        }
        if ((moiTel == null || moiTel.isEmpty())
                && ClientHandlerAuth.getInstance().getUtilisateurConnecte() != null) {
            Utilisateur u = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
            moiTel = u.getNumeroTelephone() != null ? u.getNumeroTelephone().trim() : null;
        }
        if (moiTel == null || moiTel.isEmpty()) return null;
        for (Message msg : messages) {
            String tel = msg.getTelephoneExpediteur();
            if (!numeroContactUtilisable(tel)) continue;
            String t = tel.trim();
            if (!t.equals(moiTel)) return t;
        }
        return null;
    }

    private void scrollToBottom() {
        scrollPane.setVvalue(1.0);
    }

    public static HBox makeConvItem(String name, String numero, String last,
                                    String color, int idConve, int nbNonLus) {
        StackPane avatar = makeAvatar(
                String.valueOf(name.charAt(0)).toUpperCase(), color);
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#111b21"));

        Label lastLabel = new Label(last);
        lastLabel.setFont(Font.font("Segoe UI", 12));
        lastLabel.setTextFill(Color.web("#667781"));

        VBox info = new VBox(3, nameLabel, lastLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-cursor: hand;");
        row.setUserData(idConve + ";" + numero + ";" + name);
        row.getChildren().addAll(avatar, info);

        if (nbNonLus > 0) {
            Circle badge = new Circle(10);
            badge.setFill(Color.web("#25D366"));
            Label badgeLabel = new Label(String.valueOf(nbNonLus));
            badgeLabel.setTextFill(Color.WHITE);
            badgeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            StackPane badgePane = new StackPane(badge, badgeLabel);
            row.getChildren().add(badgePane);
        }

        row.setOnMouseEntered(e -> {
            if (!row.getStyle().contains("#E8F5E9"))
                row.setStyle("-fx-background-color: #F1F8E9; -fx-cursor: hand;");
        });
        row.setOnMouseExited(e -> {
            if (!row.getStyle().contains("#E8F5E9"))
                row.setStyle("-fx-cursor: hand;");
        });

        return row;
    }

    public static HBox makeConvItem(String name, String numero, String last, String color) {
        return makeConvItem(name, numero, last, color, -1, 0);
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
            if (n instanceof Text) ((Text) n).setText(letter);
        });
    }

    private static void styleIconBtn(Button btn, String base, String hover) {
        String s1 = "-fx-background-color:" + base + ";-fx-text-fill:white;" +
                "-fx-font-size:15px;-fx-background-radius:50%;" +
                "-fx-min-width:38px;-fx-min-height:38px;-fx-cursor:hand;";
        String s2 = "-fx-background-color:" + hover + ";-fx-text-fill:white;" +
                "-fx-font-size:15px;-fx-background-radius:50%;" +
                "-fx-min-width:38px;-fx-min-height:38px;-fx-cursor:hand;";
        btn.setStyle(s1);
        btn.setOnMouseEntered(e -> btn.setStyle(s2));
        btn.setOnMouseExited(e -> btn.setStyle(s1));
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ── Méthodes appel audio ──────────────────────────────────────────────────

    private void demarrerAppelAudio(String numeroContact, String nomContact) {
        // appeler() = méthode correcte dans ClientHandlerAuth
        ClientHandlerAuth.getInstance().appeler(
                numeroContact,
                idConversationActive != null ? idConversationActive : -1,
                "AUDIO"
        );
        afficherFenetreAppel(nomContact, true, numeroContact, null);
    }
     //pour appel video
     private void demarrerAppelVideo(String numeroContact, String nomContact) {
         typeAppelEnCours = "VIDEO";
         ClientHandlerAuth.getInstance().appeler(
                 numeroContact,
                 idConversationActive != null ? idConversationActive : -1,
                 "VIDEO"
         );
         afficherFenetreAttenteVideo(nomContact);
     }
     //
     private void afficherFenetreAttenteVideo(String nomContact) {
         if (stageAppel != null && stageAppel.isShowing()) return;

         stageAppel = new Stage();
         stageAppel.initModality(javafx.stage.Modality.WINDOW_MODAL);
         stageAppel.initOwner(primaryStage);
         stageAppel.setTitle("Appel vidéo — " + nomContact);
         stageAppel.setResizable(false);

         Circle cercle = new Circle(45);
         cercle.setFill(Color.web("#25D366"));
         Text initiale = new Text(String.valueOf(nomContact.charAt(0)).toUpperCase());
         initiale.setFill(Color.WHITE);
         initiale.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
         StackPane avatarGrand = new StackPane(cercle, initiale);

         Label nomLabel = new Label(nomContact);
         nomLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
         nomLabel.setTextFill(Color.web("#111B21"));

         statutAppelLabel = new Label("Appel vidéo en cours...");
         statutAppelLabel.setFont(Font.font("Segoe UI", 14));
         statutAppelLabel.setTextFill(Color.web("#667781"));

         VBox infoBox = new VBox(16, avatarGrand, nomLabel, statutAppelLabel);
         infoBox.setAlignment(Pos.CENTER);
         infoBox.setPadding(new Insets(20, 0, 10, 0));

         Button btnRaccrocher = makeBtnAppel("📵", "#EA2424");
         btnRaccrocher.setOnAction(e -> {
             ClientHandlerAuth.getInstance().raccrocher();
             typeAppelEnCours = null;
             if (stageAppel != null) { stageAppel.close(); stageAppel = null; }
         });

         HBox boutonsBox = new HBox(btnRaccrocher);
         boutonsBox.setAlignment(Pos.CENTER);
         boutonsBox.setPadding(new Insets(20));

         VBox root = new VBox(infoBox, boutonsBox);
         root.setAlignment(Pos.CENTER);
         root.setPadding(new Insets(20));
         root.setStyle("-fx-background-color: #F0F2F5;");

         stageAppel.setScene(new javafx.scene.Scene(root, 320, 320));
         stageAppel.show();
     }
    private void afficherFenetreAppel(String nomContact, boolean estSortant,
                                      String numeroContact, String ipDistant) {
        if (stageAppel != null && stageAppel.isShowing()) return;

        stageAppel = new Stage();
        stageAppel.initModality(javafx.stage.Modality.WINDOW_MODAL);
        stageAppel.initOwner(primaryStage);
        stageAppel.setTitle(estSortant ? "Appel audio — " + nomContact : "Appel entrant");
        stageAppel.setResizable(false);
        // PAR
        Circle cercle = new Circle(45);
        cercle.setFill(Color.web("#25D366"));
        Text initiale = new Text(String.valueOf(nomContact.charAt(0)).toUpperCase());
        initiale.setFill(Color.WHITE);
        initiale.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        StackPane avatarGrand = new StackPane(cercle, initiale);

        Label nomLabel = new Label(nomContact);
        nomLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        nomLabel.setTextFill(Color.web("#111B21"));

        statutAppelLabel = new Label(estSortant ? "Appel en cours..." : "Appel entrant...");
        statutAppelLabel.setFont(Font.font("Segoe UI", 14));
        statutAppelLabel.setTextFill(Color.web("#667781"));

        VBox infoBox = new VBox(16, avatarGrand, nomLabel, statutAppelLabel);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(20, 0, 10, 0));
        HBox boutonsBox;

        if (estSortant) {
            Button btnRaccrocher = makeBtnAppel("📵", "#EA2424");
            btnRaccrocher.setOnAction(e -> terminerAppel(numeroContact));
            boutonsBox = new HBox(btnRaccrocher);
            boutonsBox.setAlignment(Pos.CENTER);
        } else {
            Button btnAccepter = makeBtnAppel("📞", "#25D366");
            btnAccepter.setOnAction(e -> {
                // accepterAppel() sans paramètre = méthode correcte
                ClientHandlerAuth.getInstance().accepterAppel();
                statutAppelLabel.setText("Appel en cours...");
               // audioUDP = new AudioUDP();
                //audioUDP.demarrer(
                        //ipDistant != null ? ipDistant : numeroContact, 6000, 6001);
                if (ipDistant != null && !ipDistant.isBlank()) {
                            audioUDP = new AudioUDP();
                            audioUDP.demarrer(ipDistant, 6001, 6000); // ✅ MODIFIÉ — ports inversés par rapport à l'appelant
                            System.out.println("[Audio] Démarré côté appelé → " + ipDistant);
                        } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur audio",
                            "IP de l'appelant non reçue. Audio impossible.");
                    ClientHandlerAuth.getInstance().refuserAppel();
                    stageAppel.close();
                    stageAppel = null;
                    return;
                }
                VBox root = (VBox) stageAppel.getScene().getRoot();
                Button btnRacc = makeBtnAppel("📵", "#EA2424");
                btnRacc.setOnAction(ev -> terminerAppel(numeroContact));
                HBox newBoutons = new HBox(btnRacc);
                newBoutons.setAlignment(Pos.CENTER);
                newBoutons.setPadding(new Insets(20));
                root.getChildren().set(root.getChildren().size() - 1, newBoutons);
            });

            Button btnRefuser = makeBtnAppel("📵", "#EA2424");
            btnRefuser.setOnAction(e -> {
                // refuserAppel() sans paramètre = méthode correcte
                ClientHandlerAuth.getInstance().refuserAppel();
                stageAppel.close();
                stageAppel = null;
            });

            boutonsBox = new HBox(30, btnAccepter, btnRefuser);
            boutonsBox.setAlignment(Pos.CENTER);
        }

        boutonsBox.setPadding(new Insets(20));
        VBox root = new VBox(infoBox, boutonsBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stageAppel.setScene(new javafx.scene.Scene(root, 320, 320));
        stageAppel.show();
    }
    private void terminerAppel(String numeroContact) {
        // raccrocher() = méthode correcte dans ClientHandlerAuth
        ClientHandlerAuth.getInstance().raccrocher();
        if (audioUDP != null) { audioUDP.arreter(); audioUDP = null; }
        if (stageAppel != null) { stageAppel.close(); stageAppel = null; }
    }
    private Button makeBtnAppel(String icone, String couleur) {
        Button btn = new Button(icone);
        btn.setFont(Font.font("Segoe UI", 20));
        btn.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 60px;" +
                        "-fx-min-height: 60px;" +
                        "-fx-cursor: hand;"
        );
        return btn;
    }
    private String trouverNomContact(String numero) {
        for (HBox item : convList.getItems()) {
            Object ud = item.getUserData();
            if (ud == null) continue;
            String[] parts = ((String) ud).split(";", 3);
            if (parts.length >= 3 && parts[1].trim().equals(numero))
                return parts[2];
        }
        return numero;
    }
}