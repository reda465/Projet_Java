package javafx;

import client.AudioUDP;
import client.ClientHandlerAuth;
import client.EcouteurClient;
import client.VideoUDP;
import model.*;
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
import service.Fileservice;
import service.FileTransferService;
import service.VoiceRecorder;
import util.GroupeFichierPayload;
import util.SqlMessageTypeUtil;
import java.io.File;
import java.util.Base64;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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
    private ListView<HBox> convList;
    private ListView<HBox> groupesList;
    private boolean ongletGroupesActif = false;
    private String typeAppelEnCours = null;
    private Fileservice fileService;
    private Button attachBtn;
    private Button btnMic;
    private final Map<Integer, Consumer<File>> callbacksFichierParMessage = new ConcurrentHashMap<>();
    private Groupe groupeActif = null;
    private boolean estEnGroupe = false;
    private MenuButton groupMenu;
    private Button btnAppelAudio;
    private Button btnAppelVideo;

    /** Données complètes pour filtrer la barre de recherche sans perdre les entrées masquées */
    private final List<Conversation> cacheConversations = new ArrayList<>();
    private final List<Groupe> cacheGroupes = new ArrayList<>();
    private TextField champRecherche;

    /** Contacts issus du serveur (GET_CONTACTS) — utilisé pour les groupes et cohérent avec la BDD */
    private final List<Contact> mesContacts = new ArrayList<>();

    public Discussion(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }

    public Scene creerScene(Stage stage) {
        this.primaryStage = stage;
        this.fileService = new Fileservice(ClientHandlerAuth.getInstance().getClientReseau());

        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(300);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dadada; -fx-border-width: 0 1 0 0;");

        HBox sideHeader = new HBox(10);
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        sideHeader.setPadding(new Insets(10, 16, 10, 16));
        sideHeader.setStyle("-fx-background-color: #25D366;");

        String initiale = utilisateurConnecte != null ?
                String.valueOf(utilisateurConnecte.getNomComplet().charAt(0)).toUpperCase() : "?";
        StackPane userAvatar = makeAvatar(initiale, "#075E54");

        Label userName = new Label(utilisateurConnecte != null ? utilisateurConnecte.getNomComplet() : "Utilisateur");
        userName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        userName.setTextFill(Color.WHITE);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button actionBtn = new Button("👤+");
        styleIconBtn(actionBtn, "#25D366", "#128C7E");

        sideHeader.getChildren().addAll(userAvatar, userName, spacerHeader, actionBtn);

        TextField search = new TextField();
        this.champRecherche = search;
        search.setPromptText("🔍  Rechercher");
        search.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        HBox searchBox = new HBox(search);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 10, 8, 10));
        search.textProperty().addListener((obs, o, n) -> {
            if (ongletGroupesActif) reconstruireListeGroupesAffichee();
            else reconstruireListeConversationsAffichee();
        });

        convList = new ListView<>();
        VBox.setVgrow(convList, Priority.ALWAYS);
        convList.setOnMouseClicked(e -> {
            HBox selected = convList.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getUserData() == null) return;
            String[] parts = selected.getUserData().toString().split(";", 3);
            try {
                ouvrirConversation(Integer.parseInt(parts[0]), parts[1], parts[2]);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        groupesList = new ListView<>();
        groupesList.setVisible(false);
        groupesList.setManaged(false);
        VBox.setVgrow(groupesList, Priority.ALWAYS);
        groupesList.setOnMouseClicked(e -> {
            HBox selected = groupesList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Object ud = selected.getUserData();
            if (ud instanceof Groupe g) ouvrirGroupe(g);
        });

        ToggleButton tabConv = new ToggleButton("💬 Conversations");
        ToggleButton tabGroup = new ToggleButton("👥 Groupes");
        ToggleGroup tg = new ToggleGroup();
        tabConv.setToggleGroup(tg); tabGroup.setToggleGroup(tg);
        tabConv.setSelected(true);
        tabConv.setStyle("-fx-background-color:#25D366; -fx-text-fill:white;");
        tabGroup.setStyle("-fx-background-color:#f0f0f0;");

        tabConv.setOnAction(e -> {
            ongletGroupesActif = false;
            convList.setVisible(true); convList.setManaged(true);
            groupesList.setVisible(false); groupesList.setManaged(false);
            tabConv.setStyle("-fx-background-color:#25D366; -fx-text-fill:white;");
            tabGroup.setStyle("-fx-background-color:#f0f0f0;");
            actionBtn.setText("👤+");
            ClientHandlerAuth.getInstance().demanderConversations();
            reconstruireListeConversationsAffichee();
        });
        tabGroup.setOnAction(e -> {
            ongletGroupesActif = true;
            convList.setVisible(false); convList.setManaged(false);
            groupesList.setVisible(true); groupesList.setManaged(true);
            tabConv.setStyle("-fx-background-color:#f0f0f0;");
            tabGroup.setStyle("-fx-background-color:#25D366; -fx-text-fill:white;");
            actionBtn.setText("👥+");
            ClientHandlerAuth.getInstance().demanderListeGroupes();
            ClientHandlerAuth.getInstance().demanderContacts();
            reconstruireListeGroupesAffichee();
        });

        actionBtn.setOnAction(e -> {
            if (!ongletGroupesActif) {
                Ajouter_contacte.show(stage, convList, ClientHandlerAuth.getInstance());
            } else {
                ClientHandlerAuth.getInstance().demanderContacts();
                List<Contact> contacts = extraireContactsDepuisConversations();
                if (contacts.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Groupe", "Aucun contact disponible."); return; }
                CreerGroupeDialog dialog = new CreerGroupeDialog(contacts);
                dialog.showAndWait().ifPresent(res -> {
                    if (res.nomGroupe != null && !res.nomGroupe.isBlank() && res.numeros.length >= 2) {
                        ClientHandlerAuth.getInstance().creerGroupe(res.nomGroupe.trim(), res.numeros);
                    }
                });
            }
        });

        sidebar.getChildren().addAll(sideHeader, searchBox, new HBox(5, tabConv, tabGroup), convList, groupesList);

        VBox chatPanel = new VBox(0);
        chatPanel.setStyle("-fx-background-color: #e5ddd5;");
        VBox.setVgrow(chatPanel, Priority.ALWAYS);

        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(10, 16, 10, 16));
        chatHeader.setStyle("-fx-background-color: #25D366;");

        chatAvatar = makeAvatar("?", "#dfe5e7");
        chatName = new Label("");
        chatName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        chatName.setTextFill(Color.WHITE);
        chatStatus = new Label("");
        chatStatus.setFont(Font.font("Segoe UI", 12));
        chatStatus.setTextFill(Color.WHITE);
        VBox chatInfo = new VBox(2, chatName, chatStatus);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnAppelAudio = new Button("📞");
        styleIconBtn(btnAppelAudio, "#25D366", "#128C7E");
        btnAppelAudio.setOnAction(e -> demarrerAppelAudio(contactActif, chatName.getText()));

        btnAppelVideo = new Button("📹");
        styleIconBtn(btnAppelVideo, "#25D366", "#128C7E");
        btnAppelVideo.setOnAction(e -> demarrerAppelVideo(contactActif, chatName.getText()));

        groupMenu = new MenuButton("⋮");
        groupMenu.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px;");
        groupMenu.setVisible(false); groupMenu.setManaged(false);
        GroupeDiscussionActions actionsGroupe = new GroupeDiscussionActions() {
            @Override public void ajouterMembreAuGroupe(int idGroupe, String numeroTelephone) {
                ClientHandlerAuth.getInstance().ajouterMembreAuGroupe(idGroupe, numeroTelephone);
            }
            @Override public void retirerMembreDuGroupe(int idGroupe, String numeroTelephone) {
                ClientHandlerAuth.getInstance().retirerMembreDuGroupe(idGroupe, numeroTelephone);
            }
            @Override public void modifierNomGroupe(int idGroupe, String nouveauNom) {
                ClientHandlerAuth.getInstance().modifierNomGroupe(idGroupe, nouveauNom);
            }
            @Override public void quitterGroupe(int idGroupe) {
                ClientHandlerAuth.getInstance().quitterGroupe(idGroupe);
            }
            @Override public void supprimerGroupe(int idGroupe) {
                ClientHandlerAuth.getInstance().supprimerGroupe(idGroupe);
            }
        };
        DiscussionGroupeFX.configurerMenu(groupMenu, primaryStage, () -> groupeActif, () -> extraireContactsDepuisConversations(), actionsGroupe, this::afficherAccueil);

        chatHeader.getChildren().addAll(chatAvatar, chatInfo, spacer, btnAppelAudio, btnAppelVideo, groupMenu);

        messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(15));
        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox inputBar = new HBox(8);
        inputBar.setPadding(new Insets(10));
        inputBar.setAlignment(Pos.CENTER);
        attachBtn = new Button("📎");
        styleIconBtn(attachBtn, "#25D366", "#128C7E");
        attachBtn.setDisable(true);
        attachBtn.setOnAction(e -> handleAttachFile());

        btnMic = new Button("\uD83C\uDFA4");
        styleIconBtn(btnMic, "#25D366", "#128C7E");
        btnMic.setDisable(true);
        btnMic.setOnAction(e -> handleVoiceRecord());

        msgField = new TextField();
        msgField.setPromptText("Tapez un message");
        msgField.setDisable(true);
        HBox.setHgrow(msgField, Priority.ALWAYS);

        sendBtn = new Button("➤");
        styleIconBtn(sendBtn, "#25D366", "#128C7E");
        sendBtn.setDisable(true);

        Runnable sendAction = () -> {
            String txt = msgField.getText().trim();
            if (txt.isEmpty()) return;
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (estEnGroupe && groupeActif != null) {
                ClientHandlerAuth.getInstance().envoyerMessageGroupe(groupeActif.getIdGroupe(), txt);
                ClientHandlerAuth.getInstance().demanderListeGroupes();
                ClientHandlerAuth.getInstance().demanderConversations();
            } else if (numeroContactUtilisable(contactActif)) {
                messagesBox.getChildren().add(Messagefx.Messageenvoyer(txt, time));
                ClientHandlerAuth.getInstance().envoyerMessage(contactActif, txt);
                ClientHandlerAuth.getInstance().demanderConversations();
            }
            msgField.clear();
            scrollToBottom();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        msgField.setOnAction(e -> sendAction.run());

        inputBar.getChildren().addAll(attachBtn, btnMic, msgField, sendBtn);
        chatPanel.getChildren().addAll(chatHeader, scrollPane, inputBar);

        afficherAccueil();

        HBox root = new HBox(sidebar, chatPanel);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);
        return new Scene(root, 900, 620);
    }

    private void handleAttachFile() {
        boolean okGroupe = estEnGroupe && groupeActif != null;
        boolean okContact = numeroContactUtilisable(contactActif);
        if (!okGroupe && !okContact) return;
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir un fichier");
        File f = chooser.showOpenDialog(primaryStage);
        if (f == null) return;
        TextInputDialog leg = new TextInputDialog();
        leg.setTitle("Légende");
        leg.setHeaderText(null);
        leg.setContentText("Texte sous le fichier (optionnel) :");
        Optional<String> cap = leg.showAndWait();
        String caption = cap.orElse("").trim();

        ProgressIndicator pi = Messagefx.petitProgress();
        attachBtn.setGraphic(pi);
        attachBtn.setDisable(true);
        if (btnMic != null) btnMic.setDisable(true);

        new Thread(() -> {
            try {
                FileTransferService fts = ClientHandlerAuth.getInstance().getFileTransferService();
                if (fts == null) throw new IllegalStateException("Service fichier indisponible");
                var res = okGroupe
                        ? fts.envoyerVersGroupe(groupeActif.getIdGroupe(), f,
                        FileTransferService.detecterTypeMessage(f), caption, p -> {
                        })
                        : fts.envoyerVersContact(contactActif, f,
                        FileTransferService.detecterTypeMessage(f), caption, p -> {
                        });
                Platform.runLater(() -> {
                    attachBtn.setGraphic(null);
                    attachBtn.setDisable(false);
                    if (btnMic != null) btnMic.setDisable(false);
                    majEtatBoutonEnvoi();
                    if (!res.succes()) {
                        showAlert(Alert.AlertType.ERROR, "Fichier", res.erreur() != null ? res.erreur() : "Échec envoi");
                    } else {
                        if (okGroupe) ClientHandlerAuth.getInstance().demanderMessagesGroupe(groupeActif.getIdGroupe());
                        else if (idConversationActive != null)
                            ClientHandlerAuth.getInstance().demanderMessages(idConversationActive);
                        ClientHandlerAuth.getInstance().demanderConversations();
                        scrollToBottom();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    attachBtn.setGraphic(null);
                    attachBtn.setDisable(false);
                    if (btnMic != null) btnMic.setDisable(false);
                    majEtatBoutonEnvoi();
                    showAlert(Alert.AlertType.ERROR, "Fichier", ex.getMessage() != null ? ex.getMessage() : "Erreur");
                });
            }
        }).start();
    }

    private void handleVoiceRecord() {
        boolean okGroupe = estEnGroupe && groupeActif != null;
        boolean okContact = numeroContactUtilisable(contactActif);
        if (!okGroupe && !okContact) return;
        chatStatus.setText("Enregistrement 8 s…");
        attachBtn.setDisable(true);
        if (btnMic != null) btnMic.setDisable(true);
        new Thread(() -> {
            try {
                File wav = File.createTempFile("voice_", ".wav");
                VoiceRecorder.enregistrerVersFichier(wav, 8000);
                FileTransferService fts = ClientHandlerAuth.getInstance().getFileTransferService();
                if (fts == null) throw new IllegalStateException("Service fichier indisponible");
                String caption = msgField.getText() != null ? msgField.getText().trim() : "";
                var res = okGroupe
                        ? fts.envoyerVersGroupe(groupeActif.getIdGroupe(), wav, "audio", caption, p -> {
                })
                        : fts.envoyerVersContact(contactActif, wav, "audio", caption, p -> {
                });
                Platform.runLater(() -> {
                    chatStatus.setText(estEnGroupe ? (groupeActif.getNumerosMembres().size() + " membres") : "en ligne");
                    msgField.clear();
                    attachBtn.setDisable(false);
                    if (btnMic != null) btnMic.setDisable(false);
                    majEtatBoutonEnvoi();
                    if (!res.succes()) {
                        showAlert(Alert.AlertType.ERROR, "Vocal", res.erreur() != null ? res.erreur() : "Échec");
                    } else {
                        if (okGroupe) ClientHandlerAuth.getInstance().demanderMessagesGroupe(groupeActif.getIdGroupe());
                        else if (idConversationActive != null)
                            ClientHandlerAuth.getInstance().demanderMessages(idConversationActive);
                        ClientHandlerAuth.getInstance().demanderConversations();
                        scrollToBottom();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    chatStatus.setText(estEnGroupe ? "membres" : "en ligne");
                    attachBtn.setDisable(false);
                    if (btnMic != null) btnMic.setDisable(false);
                    majEtatBoutonEnvoi();
                    showAlert(Alert.AlertType.ERROR, "Vocal", ex.getMessage() != null ? ex.getMessage() : "Erreur");
                });
            }
        }).start();
    }

    private void ouvrirGroupe(Groupe g) {
        this.groupeActif = g; this.estEnGroupe = true; this.contactActif = null; this.idConversationActive = null;
        chatName.setText(g.getNomGroupe());
        int nb = g.getNumerosMembres() != null ? g.getNumerosMembres().size() : 0;
        chatStatus.setText(nb + " membres");
        mettreAJourAvatar(chatAvatar, String.valueOf(g.getNomGroupe().charAt(0)).toUpperCase(), "#25D366");
        messagesBox.getChildren().clear();
        msgField.setDisable(false); majEtatBoutonEnvoi();
        btnAppelAudio.setVisible(false); btnAppelAudio.setManaged(false);
        btnAppelVideo.setVisible(false); btnAppelVideo.setManaged(false);
        groupMenu.setVisible(true); groupMenu.setManaged(true);
        ClientHandlerAuth.getInstance().demanderMessagesGroupe(g.getIdGroupe());
    }

    private void ouvrirConversation(int id, String num, String nom) {
        this.idConversationActive = id; this.contactActif = normaliserNumeroContact(num);
        this.estEnGroupe = false; this.groupeActif = null;
        chatName.setText(nom); chatStatus.setText("en ligne");
        mettreAJourAvatar(chatAvatar, String.valueOf(nom.charAt(0)).toUpperCase(), "#25D366");
        messagesBox.getChildren().clear();
        msgField.setDisable(false); majEtatBoutonEnvoi();
        btnAppelAudio.setVisible(true); btnAppelAudio.setManaged(true);
        btnAppelVideo.setVisible(true); btnAppelVideo.setManaged(true);
        groupMenu.setVisible(false); groupMenu.setManaged(false);
        if (id != -1) ClientHandlerAuth.getInstance().demanderMessages(id);
    }

    private void majEtatBoutonEnvoi() {
        boolean ok = estEnGroupe ? (groupeActif != null) : numeroContactUtilisable(contactActif);
        if (sendBtn != null) sendBtn.setDisable(!ok);
        if (attachBtn != null) attachBtn.setDisable(!ok);
        if (btnMic != null) btnMic.setDisable(!ok);
    }

    @Override public void messageRecu(String num, String contenu) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            messagesBox.getChildren().add(Messagefx.Messagerecu(contenu, time));
            if (!num.equals(contactActif)) mettreAJourBadgeNonLu(num);
            scrollToBottom();
            ClientHandlerAuth.getInstance().demanderConversations();
            ClientHandlerAuth.getInstance().demanderContacts();
        });
    }

    @Override public void messageGroupeRecu(MessageGroupe m) {
        Platform.runLater(() -> {
            if (estEnGroupe && groupeActif != null && m.getIdGroupe() == groupeActif.getIdGroupe()) {
                Utilisateur moi = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
                boolean estMoi = moi != null && m.getTelephoneExpediteur().equals(moi.getNumeroTelephone());
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                if (GroupeFichierPayload.estFichier(m.getContenu())) {
                    GroupeFichierPayload.Meta meta = GroupeFichierPayload.parser(m.getContenu());
                    if (meta != null) {
                        ajouterBulleFichierGroupe(m.getIdMessage(), meta, time, estMoi);
                    }
                } else {
                    if (estMoi) {
                        messagesBox.getChildren().add(Messagefx.Messageenvoyer(m.getContenu(), time));
                    } else {
                        messagesBox.getChildren().add(Messagefx.Messagerecu(m.getNomExpediteur() + ": " + m.getContenu(), time));
                    }
                }
                scrollToBottom();
            }
            ClientHandlerAuth.getInstance().demanderConversations();
            ClientHandlerAuth.getInstance().demanderListeGroupes();
        });
    }

    @Override public void groupeCree(Groupe g) { Platform.runLater(() -> { showAlert(Alert.AlertType.INFORMATION, "Groupe", "Groupe créé"); ClientHandlerAuth.getInstance().demanderListeGroupes(); }); }
    @Override public void creationGroupeEchouee(String r) { Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Groupe", r)); }
    @Override public void listeGroupesRecue(List<Groupe> gs) {
        Platform.runLater(() -> {
            cacheGroupes.clear();
            if (gs != null) cacheGroupes.addAll(gs);

            if (estEnGroupe && groupeActif != null && gs != null) {
                for (Groupe g : gs) {
                    if (g.getIdGroupe() == groupeActif.getIdGroupe()) {
                        groupeActif.setNumerosMembres(g.getNumerosMembres() != null ? new ArrayList<>(g.getNumerosMembres()) : new ArrayList<>());
                        int nb = groupeActif.getNumerosMembres() != null ? groupeActif.getNumerosMembres().size() : 0;
                        chatStatus.setText(nb + " membres");
                        break;
                    }
                }
            }
            reconstruireListeGroupesAffichee();
        });
    }

    @Override public void membreAjoute(int id, String n) { refreshGroupUI(null); }
    @Override public void membreRetire(int id, String n) { refreshGroupUI(null); }
    @Override public void aQuitteGroupe(int id) { refreshGroupUI(null); }
    @Override public void groupeSupprime(int id) { refreshGroupUI("Groupe supprimé"); }
    @Override public void nomGroupeModifie(int id, String n) { refreshGroupUI(null); }

    private void refreshGroupUI(String msg) {
        Platform.runLater(() -> {
            if (msg != null) showAlert(Alert.AlertType.INFORMATION, "Groupe", msg);
            ClientHandlerAuth.getInstance().demanderListeGroupes();
        });
    }

    @Override public void conversationsRecues(List<Conversation> cs) {
        Platform.runLater(() -> {
            cacheConversations.clear();
            if (cs != null) cacheConversations.addAll(cs);
            reconstruireListeConversationsAffichee();
        });
    }

    private static boolean correspondRecherche(String texte, String requete) {
        if (requete == null || requete.isBlank()) return true;
        if (texte == null) return false;
        return texte.toLowerCase().contains(requete.trim().toLowerCase());
    }

    private void reconstruireListeConversationsAffichee() {
        if (convList == null) return;
        String q = champRecherche != null ? champRecherche.getText() : "";
        convList.getItems().clear();
        for (Conversation c : cacheConversations) {
            String nom = c.getNomContact() != null ? c.getNomContact() : "";
            String num = c.getNumeroContact() != null ? c.getNumeroContact() : "";
            if (!correspondRecherche(nom, q) && !correspondRecherche(num, q)) continue;
            String dernier = c.getDernierMessage() != null ? c.getDernierMessage() : "";
            convList.getItems().add(makeConvItem(
                    nom.isEmpty() ? "?" : nom,
                    num,
                    dernier,
                    "#128C7E",
                    c.getIdConversation(),
                    c.getMessagesNonLus()));
        }
    }

    private void reconstruireListeGroupesAffichee() {
        if (groupesList == null) return;
        String q = champRecherche != null ? champRecherche.getText() : "";
        groupesList.getItems().clear();
        for (Groupe g : cacheGroupes) {
            String nom = g.getNomGroupe() != null ? g.getNomGroupe() : "";
            if (!correspondRecherche(nom, q)) continue;
            HBox item = makeConvItem(nom.isEmpty() ? "?" : nom, "", "Groupe", "#25D366", g.getIdGroupe(), 0);
            item.setUserData(g);
            groupesList.getItems().add(item);
        }
    }

    @Override public void messageFichierNotifie(String telephoneExpediteur, int idMessage, String nomFichier,
                                               String typeMessage, long tailleOctets, String legende) {
        Platform.runLater(() -> {
            if (!numeroContactUtilisable(contactActif) || estEnGroupe
                    || !telephoneExpediteur.equals(contactActif)) {
                mettreAJourBadgeNonLu(telephoneExpediteur);
                ClientHandlerAuth.getInstance().demanderConversations();
                return;
            }
            Message m = new Message() {
                @Override
                public String toNetworkString() {
                    return "";
                }
            };
            m.setIdMessage(idMessage);
            m.setEstMoi(false);
            m.setTypeMessage(typeMessage);
            m.setNomFichier(nomFichier);
            m.setTailleFichier(tailleOctets);
            m.setContenuTexte(legende != null ? legende : "");
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            ajouterBulleFichierConversation(m, time);
            scrollToBottom();
            ClientHandlerAuth.getInstance().demanderConversations();
        });
    }

    @Override public void fichierTelecharge(int messageId, File fichierLocal, String typeMessage) {
        Platform.runLater(() -> {
            Consumer<File> cb = callbacksFichierParMessage.remove(messageId);
            if (cb != null) cb.accept(fichierLocal);
        });
    }

    @Override public void fichierTelechargeErreur(int messageId, String raison) {
        Platform.runLater(() -> {
            callbacksFichierParMessage.remove(messageId);
            showAlert(Alert.AlertType.ERROR, "Téléchargement", raison != null ? raison : "Erreur");
        });
    }

    @Override public void uploadFichierResultat(String sessionId, boolean succes, String mode, int idMessage, String erreur) {
        // Optionnel : logs / toast
    }

    @Override public void messagesRecus(List<Message> ms) {
        Platform.runLater(() -> {
            messagesBox.getChildren().clear();
            if (ms == null) return;
            for (Message m : ms) {
                ajouterLigneHistoriqueMessage(m);
            }
            if (!numeroContactUtilisable(contactActif)) contactActif = infererNumeroInterlocuteur(ms);
            scrollToBottom();
        });
    }

    @Override public void fichierRecu(String tel, String name, String b64) {
        Platform.runLater(() -> {
            try {
                byte[] data = Base64.getDecoder().decode(b64);
                File out = new File("downloads/" + name); out.getParentFile().mkdirs();
                java.nio.file.Files.write(out.toPath(), data);
                messagesBox.getChildren().add(Messagefx.Messagerecu("📎 " + name, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
                scrollToBottom();
                ClientHandlerAuth.getInstance().demanderConversations();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override public void appelEntrant(String num, String type, String ip, String name) {
        Platform.runLater(() -> {
            if ("VIDEO".equalsIgnoreCase(type)) { typeAppelEnCours = "VIDEO"; Appelvideo.recevoirAppel(primaryStage, name, num, ip); }
            else { typeAppelEnCours = "AUDIO"; afficherFenetreAppel(name, false, num, ip); }
        });
    }

    @Override public void appelAccepte(String num, String ip) {
        Platform.runLater(() -> {
            if ("VIDEO".equalsIgnoreCase(typeAppelEnCours)) {
                if (stageAppel != null) stageAppel.close();
                Appelvideo.demarrer(primaryStage, chatName.getText(), contactActif, idConversationActive != null ? idConversationActive : -1, ip);
            } else {
                audioUDP = new AudioUDP(); audioUDP.demarrer(ip, 6000, 6001);
                if (statutAppelLabel != null) statutAppelLabel.setText("En communication...");
            }
        });
    }

    @Override public void appelRefuse() { Platform.runLater(() -> { if (stageAppel != null) stageAppel.close(); showAlert(Alert.AlertType.INFORMATION, "Appel", "Refusé"); }); }
    @Override public void appelTermine(String n) { Platform.runLater(() -> { if (audioUDP != null) audioUDP.arreter(); if (stageAppel != null) stageAppel.close(); typeAppelEnCours = null; }); }

    private void demarrerAppelAudio(String num, String nom) {
        if (!numeroContactUtilisable(num)) return;
        ClientHandlerAuth.getInstance().appeler(num, idConversationActive != null ? idConversationActive : -1, "AUDIO");
        afficherFenetreAppel(nom, true, num, null);
    }

    private void demarrerAppelVideo(String num, String nom) {
        if (!numeroContactUtilisable(num)) return;
        typeAppelEnCours = "VIDEO";
        ClientHandlerAuth.getInstance().appeler(num, idConversationActive != null ? idConversationActive : -1, "VIDEO");
        afficherFenetreAttenteVideo(nom);
    }

    private void afficherFenetreAttenteVideo(String nom) {
        stageAppel = new Stage();
        VBox root = new VBox(20, makeAvatar(String.valueOf(nom.charAt(0)), "#25D366"), new Label(nom), new Label("Appel vidéo..."));
        root.setAlignment(Pos.CENTER); root.setPadding(new Insets(20));
        Button hangup = makeBtnAppel("📵", "#EA2424");
        hangup.setOnAction(e -> { ClientHandlerAuth.getInstance().raccrocher(); stageAppel.close(); });
        root.getChildren().add(hangup);
        stageAppel.setScene(new Scene(root, 300, 350)); stageAppel.show();
    }

    private void afficherFenetreAppel(String nom, boolean sortant, String num, String ip) {
        stageAppel = new Stage();
        VBox root = new VBox(20, makeAvatar(String.valueOf(nom.charAt(0)), "#25D366"), new Label(nom), statutAppelLabel = new Label(sortant ? "Appel..." : "Appel entrant..."));
        root.setAlignment(Pos.CENTER); root.setPadding(new Insets(20));
        if (sortant) {
            Button h = makeBtnAppel("📵", "#EA2424"); h.setOnAction(e -> { ClientHandlerAuth.getInstance().raccrocher(); stageAppel.close(); });
            root.getChildren().add(h);
        } else {
            Button acc = makeBtnAppel("📞", "#25D366");
            acc.setOnAction(e -> {
                ClientHandlerAuth.getInstance().accepterAppel();
                if (ip != null) { audioUDP = new AudioUDP(); audioUDP.demarrer(ip, 6001, 6000); }
                statutAppelLabel.setText("En communication...");
            });
            Button ref = makeBtnAppel("📵", "#EA2424"); ref.setOnAction(e -> { ClientHandlerAuth.getInstance().refuserAppel(); stageAppel.close(); });
            root.getChildren().add(new HBox(20, acc, ref));
        }
        stageAppel.setScene(new Scene(root, 300, 350)); stageAppel.show();
    }

    private Button makeBtnAppel(String icon, String color) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-background-radius:50%; -fx-min-width:60px; -fx-min-height:60px;");
        return b;
    }

    private void afficherAccueil() {
        messagesBox.getChildren().clear();
        Label l = new Label("💬 Sélectionnez une discussion");
        l.setTextFill(Color.GRAY);
        VBox v = new VBox(l); v.setAlignment(Pos.CENTER); VBox.setVgrow(v, Priority.ALWAYS);
        messagesBox.getChildren().add(v);
        chatName.setText(""); chatStatus.setText("");
        btnAppelAudio.setVisible(false); btnAppelVideo.setVisible(false); groupMenu.setVisible(false);
        msgField.setDisable(true); attachBtn.setDisable(true); sendBtn.setDisable(true);
        if (btnMic != null) btnMic.setDisable(true);
    }

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

    private List<Contact> extraireContactsDepuisConversations() {
        if (!mesContacts.isEmpty()) {
            return new ArrayList<>(mesContacts);
        }
        List<Contact> res = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Conversation c : cacheConversations) {
            String tel = c.getNumeroContact();
            if (tel == null || tel.isBlank() || !seen.add(tel.trim())) continue;
            Contact contact = new Contact();
            contact.setNumeroTelephone(tel.trim());
            String nom = c.getNomContact() != null && !c.getNomContact().isBlank() ? c.getNomContact().trim() : tel;
            contact.setNomComplet(nom);
            res.add(contact);
        }
        return res;
    }

    private String infererNumeroInterlocuteur(List<Message> ms) {
        if (ms == null || ms.isEmpty() || utilisateurConnecte == null) return null;
        for (Message m : ms) {
            if (!m.getTelephoneExpediteur().equals(utilisateurConnecte.getNumeroTelephone())) return m.getTelephoneExpediteur();
        }
        return null;
    }

    private void ajouterLigneHistoriqueMessage(Message m) {
        String time = m.getDateEnvoi() != null ? m.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        if (m.getTypeMessage() == null || "texte".equals(m.getTypeMessage())
                || m.getNomFichier() == null || m.getNomFichier().isBlank()) {
            String txt = m.getContenuTexte() != null ? m.getContenuTexte() : "";
            messagesBox.getChildren().add(m.isEstMoi() ? Messagefx.Messageenvoyer(txt, time) : Messagefx.Messagerecu(txt, time));
            return;
        }
        ajouterBulleFichierConversation(m, time);
    }

    private void ajouterBulleFichierConversation(Message m, String time) {
        boolean envoye = m.isEstMoi();
        String leg = m.getContenuTexte() != null ? m.getContenuTexte() : "";
        String sous = (leg.isBlank() ? "" : leg + " · ") + m.getNomFichier() + " (" + formatTaille(m.getTailleFichier()) + ")";
        VBox center = new VBox(6);
        Button dl = Messagefx.boutonTelecharger("⬇ Télécharger");
        int mid = m.getIdMessage();
        dl.setOnAction(ev -> {
            center.getChildren().setAll(Messagefx.petitProgress());
            callbacksFichierParMessage.put(mid, file -> remplirApercuFichier(center, file, m.getTypeMessage(), sous, time, envoye));
            ClientHandlerAuth.getInstance().demanderFichierMessage(mid);
        });
        center.getChildren().add(dl);
        HBox row = Messagefx.messageFichier(envoye, center, sous, time);
        row.setUserData(mid);
        messagesBox.getChildren().add(row);
    }

    private void ajouterBulleFichierGroupe(int idMessage, GroupeFichierPayload.Meta meta, String time, boolean estMoi) {
        String sous = (meta.legende() != null && !meta.legende().isBlank() ? meta.legende() + " · " : "")
                + meta.nomFichier() + " (" + formatTaille(meta.taille()) + ")";
        VBox center = new VBox(6);
        Button dl = Messagefx.boutonTelecharger("⬇ Télécharger");
        dl.setOnAction(ev -> {
            center.getChildren().setAll(Messagefx.petitProgress());
            callbacksFichierParMessage.put(idMessage, file -> remplirApercuFichier(center, file, meta.typeMessage(), sous, time, estMoi));
            ClientHandlerAuth.getInstance().demanderFichierGroupe(idMessage);
        });
        center.getChildren().add(dl);
        HBox row = Messagefx.messageFichier(estMoi, center, sous, time);
        row.setUserData(idMessage);
        messagesBox.getChildren().add(row);
    }

    private static String formatTaille(Long t) {
        if (t == null) return "?";
        if (t < 1024) return t + " o";
        if (t < 1024 * 1024) return (t / 1024) + " Ko";
        return (t / (1024 * 1024)) + " Mo";
    }

    private void remplirApercuFichier(VBox center, File file, String type, String sous, String time, boolean envoye) {
        String eff = SqlMessageTypeUtil.pourAffichage(type, file != null ? file.getName() : null);
        center.getChildren().clear();
        try {
            if ("image".equals(eff) && file.exists()) {
                javafx.scene.image.ImageView iv = Messagefx.miniImagePreview(220, 180);
                iv.setImage(new Image(file.toURI().toString(), true));
                center.getChildren().add(iv);
            } else if ("audio".equals(eff)) {
                Button play = new Button("▶ Lire");
                play.setOnAction(e -> {
                    try {
                        Media med = new Media(file.toURI().toString());
                        MediaPlayer mp = new MediaPlayer(med);
                        mp.play();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                center.getChildren().add(play);
            } else {
                Hyperlink link = new Hyperlink("Ouvrir " + file.getName());
                link.setOnAction(e -> {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(file);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                center.getChildren().add(link);
            }
        } catch (Exception e) {
            center.getChildren().add(new Label(file.getName()));
        }
    }

    private void scrollToBottom() { Platform.runLater(() -> scrollPane.setVvalue(1.0)); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static HBox makeConvItem(String name, String num, String last, String color, int id, int unread) {
        VBox v = new VBox(2, new Label(name), new Label(last));
        HBox h = new HBox(10, makeAvatar(String.valueOf(name.charAt(0)), color), v);
        h.setPadding(new Insets(10)); h.setUserData(id + ";" + num + ";" + name);
        if (unread > 0) h.getChildren().add(new StackPane(new Circle(10, Color.GREEN), new Text(String.valueOf(unread))));
        return h;
    }

    static StackPane makeAvatar(String letter, String color) {
        Circle c = new Circle(20, Color.web(color));
        Text t = new Text(letter); t.setFill(Color.WHITE);
        return new StackPane(c, t);
    }

    private void mettreAJourAvatar(StackPane s, String letter, String color) {
        ((Circle) s.getChildren().get(0)).setFill(Color.web(color));
        ((Text) s.getChildren().get(1)).setText(letter);
    }

    private static void styleIconBtn(Button b, String base, String hover) {
        b.setStyle("-fx-background-color:" + base + "; -fx-text-fill:white; -fx-background-radius:50%; -fx-min-width:38px; -fx-min-height:38px; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:" + hover + "; -fx-text-fill:white; -fx-background-radius:50%; -fx-min-width:38px; -fx-min-height:38px; -fx-cursor:hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:" + base + "; -fx-text-fill:white; -fx-background-radius:50%; -fx-min-width:38px; -fx-min-height:38px; -fx-cursor:hand;"));
    }

    private static boolean numeroContactUtilisable(String t) { return t != null && !t.isBlank() && !"null".equalsIgnoreCase(t); }
    private static String normaliserNumeroContact(String t) { return numeroContactUtilisable(t) ? t.trim() : null; }

    @Override public void connexionReussie(Utilisateur u) { this.utilisateurConnecte = u; }
    @Override public void inscriptionReussie(String m) {}
    @Override public void erreur(String m) { Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", m)); }
    @Override public void deconnexion() { Platform.runLater(() -> showAlert(Alert.AlertType.WARNING, "Déconnexion", "Déconnecté")); }
    @Override public void contactAjoute(Contact c) {
        Platform.runLater(() -> {
            if (c == null || !numeroContactUtilisable(c.getNumeroTelephone())) return;
            String tel = c.getNumeroTelephone().trim();
            String nom = c.getNomComplet() != null && !c.getNomComplet().isBlank() ? c.getNomComplet().trim() : tel;
            for (Conversation ex : cacheConversations) {
                if (ex.getNumeroContact() != null && tel.equals(ex.getNumeroContact().trim())) {
                    assurerContactDansMesContacts(c, tel);
                    reconstruireListeConversationsAffichee();
                    return;
                }
            }
            Conversation nc = new Conversation();
            nc.setNomContact(nom);
            nc.setNumeroContact(tel);
            nc.setIdConversation(-1);
            nc.setDernierMessage("");
            nc.setMessagesNonLus(0);
            cacheConversations.add(0, nc);
            nc.setTypeConversation("individuelle");
            assurerContactDansMesContacts(c, tel);
            reconstruireListeConversationsAffichee();
        });
    }

    private void assurerContactDansMesContacts(Contact c, String tel) {
        if (c == null || tel == null) return;
        for (Contact mc : mesContacts) {
            if (mc != null && mc.getNumeroTelephone() != null && tel.equals(mc.getNumeroTelephone().trim())) {
                return;
            }
        }
        mesContacts.add(c);
    }

    @Override
    public void demandeContactRecue(String numeroDemandeur, String nomDemandeur) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Demande de contact");
            a.setHeaderText(null);
            a.setContentText((nomDemandeur != null ? nomDemandeur : numeroDemandeur)
                    + " (" + numeroDemandeur + ") souhaite être dans vos contacts.");
            ButtonType accepter = new ButtonType("Accepter");
            ButtonType bloquer = new ButtonType("Bloquer");
            ButtonType plusTard = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(accepter, bloquer, plusTard);
            Optional<ButtonType> r = a.showAndWait();
            if (r.isEmpty()) return;
            if (r.get() == accepter) ClientHandlerAuth.getInstance().accepterDemandeContact(numeroDemandeur);
            else if (r.get() == bloquer) ClientHandlerAuth.getInstance().bloquerNumeroContact(numeroDemandeur);
        });
    }

    @Override
    public void contactAcceptationConfirmee() {
        Platform.runLater(() -> {
            ClientHandlerAuth.getInstance().demanderConversations();
            ClientHandlerAuth.getInstance().demanderContacts();
        });
    }
    @Override public void listeContactsRecue(List<Contact> cs) {
        Platform.runLater(() -> {
            mesContacts.clear();
            if (cs != null) {
                mesContacts.addAll(cs);
            }
        });
    }
    @Override public void membresGroupeRecus(int id, List<Utilisateur> ms) {}
}
