package javafx;
import client.AudioUDP;
import client.ClientHandlerAuth;
import client.EcouteurClient;
import client.VideoUDP;
import model.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.Fileservice;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import util.FileMediaUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.sound.sampled.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private Button voiceBtn;
    private TextField searchField;
    private ProgressBar fileProgressBar;
    private final List<HBox> convListMaster = new ArrayList<>();
    private final List<HBox> groupesListMaster = new ArrayList<>();
    private TargetDataLine ligneVocale;
    private ByteArrayOutputStream bufferVocal;
    private Thread threadEnregistrement;
    private AudioFormat formatVocalEnCours;

    // Nouveaux champs pour les groupes
    private Groupe groupeActif = null;
    private boolean estEnGroupe = false;
    private MenuButton groupMenu;
    private Button btnAppelAudio;
    private Button btnAppelVideo;
    private AppelAudioGroupe appelAudioGroupeActif;
    private AppelVideoGroupe appelVideoGroupeActif;
    private Label appelEnCoursLabel; // Indicateur visuel "en appel" (en-tête chat)

    //Barre d'appel en cours au-dessus de la liste des contacts
    private HBox barreAppelSidebar;
    private Label labelBarreAppelSidebar;
    private Timeline chronometreAppel;
    private Instant debutAppelGroupe;
    private int idGroupeEnAppel = -1;
    private String nomGroupeEnAppel = "";
    private String typeAppelGroupeEnCours = "";

    /** Groupes connus (pour rejoindre un appel sans ouvrir la conversation) */
    private final Map<Integer, Groupe> cacheGroupes = new ConcurrentHashMap<>();
    /** Groupes dont nous avons initié l'appel — ne pas afficher la popup entrante */
    private final Set<Integer> appelsGroupeInitiateur = ConcurrentHashMap.newKeySet();
    /** Invitation en attente si la liste des groupes n'est pas encore chargée */
    private int invitationAppelIdGroupe = -1;
    private String invitationAppelType = "";
    private String invitationAppelNomGroupe = "";

    /** Contacts issus du serveur (GET_CONTACTS) — utilisé pour les groupes et cohérent avec la BDD */
    private final List<Contact> mesContacts = new ArrayList<>();

    public Discussion(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }

    public Scene creerScene(Stage stage) {
        this.primaryStage = stage;
        Messagefx.setStage(stage);
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

        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher");
        searchField.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px;");
        HBox searchBox = new HBox(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.setPadding(new Insets(8, 10, 8, 10));
        configurerRecherche();

        labelBarreAppelSidebar = new Label();
        labelBarreAppelSidebar.setTextFill(Color.WHITE);
        labelBarreAppelSidebar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        labelBarreAppelSidebar.setWrapText(true);

        Button btnRetourAppel = new Button("Rejoindre");
        btnRetourAppel.setStyle("-fx-background-color: white; -fx-text-fill: #128C7E; -fx-font-size: 11px; -fx-background-radius: 12px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        btnRetourAppel.setText("Afficher");
        btnRetourAppel.setOnAction(e -> afficherFenetreAppelGroupeActif());

        barreAppelSidebar = new HBox(8, labelBarreAppelSidebar, new Region(), btnRetourAppel);
        HBox.setHgrow(barreAppelSidebar.getChildren().get(1), Priority.ALWAYS);
        barreAppelSidebar.setAlignment(Pos.CENTER_LEFT);
        barreAppelSidebar.setPadding(new Insets(8, 12, 8, 12));
        barreAppelSidebar.setStyle("-fx-background-color: #128C7E;");
        barreAppelSidebar.setVisible(false);
        barreAppelSidebar.setManaged(false);

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
        });
        tabGroup.setOnAction(e -> {
            ongletGroupesActif = true;
            convList.setVisible(false); convList.setManaged(false);
            groupesList.setVisible(true); groupesList.setManaged(true);
            tabConv.setStyle("-fx-background-color:#f0f0f0;");
            tabGroup.setStyle("-fx-background-color:#25D366; -fx-text-fill:white;");
            actionBtn.setText("👥+");
            ClientHandlerAuth.getInstance().demanderListeGroupes();
        });

        actionBtn.setOnAction(e -> {
            if (!ongletGroupesActif) {
                Ajouter_contacte.show(stage, convList, ClientHandlerAuth.getInstance());
                return;
            }
            List<Contact> contacts = extraireContactsDepuisConversations();
            if (contacts.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Groupe", "Aucun contact disponible pour créer un groupe.");
                return;
            }
            CreerGroupeDialog dialog = new CreerGroupeDialog(contacts);
            dialog.showAndWait().ifPresent(res -> {
                if (res.nomGroupe == null || res.nomGroupe.trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Groupe", "Nom du groupe requis.");
                    return;
                }
                if (res.numeros == null || res.numeros.length < 2) {
                    showAlert(Alert.AlertType.WARNING, "Groupe", "Choisissez au moins 2 membres.");
                    return;
                }
                ClientHandlerAuth.getInstance().creerGroupe(res.nomGroupe.trim(), res.numeros);
            });
        });

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

        sidebar.getChildren().addAll(sideHeader, searchBox, barreAppelSidebar, new HBox(5, tabConv, tabGroup), convList, groupesList);

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

        // Indicateur visuel d'appel en cours (masqué par défaut)
        appelEnCoursLabel = new Label("🔴 Appel en cours");
        appelEnCoursLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-background-color: rgba(0,0,0,0.2); -fx-padding: 2 6 2 6; -fx-background-radius: 8;");
        appelEnCoursLabel.setVisible(false);

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

        chatHeader.getChildren().addAll(chatAvatar, chatInfo, spacer, appelEnCoursLabel, btnAppelAudio, btnAppelVideo, groupMenu);

        messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(15));
        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox inputBar = new HBox(8);
        inputBar.setPadding(new Insets(10,12,10,12));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color: #f0f0f0;");
        attachBtn = new Button("📎");
        styleIconBtn(attachBtn, "#25D366", "#128C7E");
        attachBtn.setTooltip(new Tooltip("Envoyer un fichier"));
        attachBtn.setDisable(true);
        attachBtn.setOnAction(e -> choisirEtEnvoyerFichier(false));
        MenuItem itemGalerie = new MenuItem("Galerie / Fichiers");
        itemGalerie.setOnAction(e -> choisirEtEnvoyerFichier(false));
        MenuItem itemCamera = new MenuItem("Appareil photo");
        itemCamera.setOnAction(e -> choisirEtEnvoyerFichier(true));
        ContextMenu menuAttach = new ContextMenu(itemGalerie, itemCamera);
        attachBtn.setOnContextMenuRequested(ev -> menuAttach.show(attachBtn, ev.getScreenX(), ev.getScreenY()));

        voiceBtn = new Button("🎤");
        styleIconBtn(voiceBtn, "#25D366", "#128C7E");
        voiceBtn.setTooltip(new Tooltip("Message vocal (maintenir pour enregistrer)"));
        voiceBtn.setDisable(true);
        configurerBoutonVocal();

        fileProgressBar = new ProgressBar(0);
        fileProgressBar.setVisible(false);
        fileProgressBar.setManaged(false);
        fileProgressBar.setPrefWidth(120);

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

        inputBar.getChildren().addAll(attachBtn, voiceBtn, msgField, fileProgressBar, sendBtn);
        chatPanel.getChildren().addAll(chatHeader, scrollPane, inputBar);

        afficherAccueil();

        HBox root = new HBox(sidebar, chatPanel);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);
        return new Scene(root, 900, 620);
    }

    private void configurerRecherche() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, old, query) -> filtrerListes());
    }

    private void filtrerListes() {
        String q = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase() : "";
        convList.getItems().clear();
        for (HBox item : convListMaster) {
            if (q.isEmpty() || correspondRechercheConv(item, q)) convList.getItems().add(item);
        }
        groupesList.getItems().clear();
        for (HBox item : groupesListMaster) {
            if (q.isEmpty() || correspondRechercheGroupe(item, q)) groupesList.getItems().add(item);
        }
    }

    private boolean correspondRechercheConv(HBox item, String q) {
        Object ud = item.getUserData();
        if (ud instanceof String s) {
            String[] p = s.split(";", 3);
            if (p.length >= 3 && (p[2].toLowerCase().contains(q) || p[1].toLowerCase().contains(q))) return true;
        }
        for (javafx.scene.Node n : item.getChildren()) {
            if (n instanceof VBox v) {
                for (javafx.scene.Node c : v.getChildren()) {
                    if (c instanceof Label lbl && lbl.getText() != null
                            && lbl.getText().toLowerCase().contains(q)) return true;
                }
            }
        }
        return false;
    }

    private boolean correspondRechercheGroupe(HBox item, String q) {
        Object ud = item.getUserData();
        if (ud instanceof Groupe g && g.getNomGroupe() != null
                && g.getNomGroupe().toLowerCase().contains(q)) return true;
        return correspondRechercheConv(item, q);
    }

    private void choisirEtEnvoyerFichier(boolean depuisCamera) {
        boolean ok = estEnGroupe ? groupeActif != null : numeroContactUtilisable(contactActif);
        if (!ok) return;

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir un fichier à envoyer");
        javafx.stage.FileChooser.ExtensionFilter tous = new javafx.stage.FileChooser.ExtensionFilter("Tous les fichiers", "*.*");
        chooser.getExtensionFilters().addAll(
                tous,
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"),
                new javafx.stage.FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.mov", "*.avi", "*.mkv", "*.webm"),
                new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.xlsx", "*.pptx"),
                new javafx.stage.FileChooser.ExtensionFilter("Audio", "*.wav", "*.mp3", "*.ogg", "*.m4a", "*.aac"));
        chooser.setSelectedExtensionFilter(tous);
        File fichier = depuisCamera ? capturerPhoto() : chooser.showOpenDialog(primaryStage);
        if (fichier != null) montrerApercuEtEnvoyer(fichier);
    }

    private File capturerPhoto() {
        try {
            com.github.sarxos.webcam.Webcam cam = com.github.sarxos.webcam.Webcam.getDefault();
            if (cam == null) {
                showAlert(Alert.AlertType.WARNING, "Caméra", "Aucune caméra détectée.");
                return null;
            }
            java.awt.image.BufferedImage img = cam.getImage();
            if (img == null) {
                cam.open();
                img = cam.getImage();
                cam.close();
            }
            if (img == null) return null;
            File out = new File("downloads/capture_" + System.currentTimeMillis() + ".png");
            out.getParentFile().mkdirs();
            javax.imageio.ImageIO.write(img, "png", out);
            return out;
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Caméra", "Impossible de capturer : " + e.getMessage());
            return null;
        }
    }

    private void montrerApercuEtEnvoyer(File fichier) {
        String type = FileMediaUtil.detectType(fichier);
        Alert preview = new Alert(Alert.AlertType.CONFIRMATION);
        preview.setTitle("Aperçu avant envoi");
        preview.setHeaderText(fichier.getName() + " (" + FileMediaUtil.labelForType(type) + ")");
        VBox box = new VBox(8);
        if ("image".equals(type)) {
            ImageView iv = new ImageView(new Image(fichier.toURI().toString(), 280, 280, true, true));
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } else {
            box.getChildren().add(new Label(FileMediaUtil.labelForType(type) + " — " + formatTaille(fichier.length())));
        }
        preview.getDialogPane().setContent(box);
        ButtonType btnEnvoyer = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        preview.getButtonTypes().setAll(btnEnvoyer, ButtonType.CANCEL);
        preview.showAndWait().ifPresent(btn -> {
            if (btn == btnEnvoyer) envoyerFichierDepuisUI(fichier, type);
        });
    }

    private void envoyerFichierDepuisUI(File fichier, String type) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        File copie = fichier;
        try {
            if (!fichier.getAbsolutePath().startsWith(dossierTelechargements().getAbsolutePath())) {
                copie = sauvegarderFichierLocal(fichier.getName(),
                        java.nio.file.Files.readAllBytes(fichier.toPath()),
                        estEnGroupe && groupeActif != null ? groupeActif.getIdGroupe() : null);
            }
        } catch (Exception ignored) {}
        ajouterBulleFichier(true, type, fichier.getName(), time, copie);
        java.util.function.Consumer<Integer> progress = this::fichierProgress;
        if (estEnGroupe && groupeActif != null) {
            ClientHandlerAuth.getInstance().envoyerFichierGroupe(groupeActif.getIdGroupe(), fichier, progress);
        } else {
            ClientHandlerAuth.getInstance().envoyerFichier(contactActif, fichier, progress);
        }
        scrollToBottom();
        ClientHandlerAuth.getInstance().demanderConversations();
        if (estEnGroupe) ClientHandlerAuth.getInstance().demanderListeGroupes();
    }

    private void traiterFichierRecu(String tel, String type, String name, String b64,
                                     boolean groupe, Integer idGroupe, String nomExp,
                                     LocalTime timeHistorique) {
        try {
            byte[] data = Base64.getDecoder().decode(b64);
            File out = sauvegarderFichierLocal(name, data, groupe ? idGroupe : null);
            String time = timeHistorique != null
                    ? timeHistorique.format(DateTimeFormatter.ofPattern("HH:mm"))
                    : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            boolean afficher = !groupe || (estEnGroupe && groupeActif != null
                    && idGroupe != null && groupeActif.getIdGroupe() == idGroupe);
            if (afficher) {
                boolean estMoi = estMonNumero(tel);
                if (groupe && !estMoi && nomExp != null) {
                    messagesBox.getChildren().add(Messagefx.MessagerecuFichier(type, nomExp + ": " + name, time, out));
                } else {
                    ajouterBulleFichier(estMoi, type, name, time, out);
                }
                if (timeHistorique == null) scrollToBottom();
            }
            if (!groupe && tel != null && !tel.equals(contactActif)) mettreAJourBadgeNonLu(tel);
            if (groupe && idGroupe != null && !afficher) {
                mettreAJourBadgeGroupeNonLu(idGroupe, name);
            }
            if (groupe) {
                ClientHandlerAuth.getInstance().demanderListeGroupes();
            } else {
                ClientHandlerAuth.getInstance().demanderConversations();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur fichier", "Impossible de recevoir : " + name);
        }
    }

    private static File dossierTelechargements() {
        File dir = new File(System.getProperty("user.dir"), "downloads");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File sauvegarderFichierLocal(String name, byte[] data, Integer idGroupe) throws java.io.IOException {
        if (data == null || data.length == 0) {
            throw new java.io.IOException("Données fichier vides");
        }
        String prefix = idGroupe != null ? "g" + idGroupe + "_" : "";
        String safeName = name != null ? name.replaceAll("[\\\\/:*?\"<>|]", "_") : "fichier";
        File out = new File(dossierTelechargements(), prefix + System.currentTimeMillis() + "_" + safeName);
        java.nio.file.Files.write(out.toPath(), data);
        return out;
    }

    private void ajouterBulleFichier(boolean estMoi, String type, String name, String time, File local) {
        if (estMoi) {
            messagesBox.getChildren().add(Messagefx.MessageenvoyerFichier(type, name, time, local));
        } else {
            messagesBox.getChildren().add(Messagefx.MessagerecuFichier(type, name, time, local));
        }
    }

    private void configurerBoutonVocal() {
        voiceBtn.setOnMousePressed(e -> {
            e.consume();
            demarrerEnregistrementVocal();
        });
        voiceBtn.setOnMouseReleased(e -> {
            e.consume();
            arreterEtEnvoyerVocal();
        });
    }

    private void demarrerEnregistrementVocal() {
        if (ligneVocale != null) return;
        try {
            formatVocalEnCours = null;
            AudioFormat[] essais = {
                    new AudioFormat(16000, 16, 1, true, false),
                    new AudioFormat(8000, 16, 1, true, false),
                    new AudioFormat(44100, 16, 1, true, false)
            };
            for (AudioFormat f : essais) {
                try {
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, f);
                    if (!AudioSystem.isLineSupported(info)) continue;
                    ligneVocale = (TargetDataLine) AudioSystem.getLine(info);
                    ligneVocale.open(f);
                    formatVocalEnCours = f;
                    break;
                } catch (Exception ignored) {}
            }
            if (ligneVocale == null) {
                AudioFormat def = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, def);
                ligneVocale = (TargetDataLine) AudioSystem.getLine(info);
                ligneVocale.open(def);
                formatVocalEnCours = def;
            }
            bufferVocal = new ByteArrayOutputStream();
            ligneVocale.start();
            byte[] buf = new byte[4096];
            TargetDataLine ligne = ligneVocale;
            threadEnregistrement = new Thread(() -> {
                try {
                    while (ligne.isOpen()) {
                        int n = ligne.read(buf, 0, buf.length);
                        if (n > 0) bufferVocal.write(buf, 0, n);
                    }
                } catch (Exception ignored) {}
            });
            threadEnregistrement.setDaemon(true);
            threadEnregistrement.start();
            voiceBtn.setStyle("-fx-background-color:#EA2424; -fx-text-fill:white; -fx-background-radius:50%; -fx-min-width:38px; -fx-min-height:38px;");
            chatStatus.setText("🎤 Enregistrement...");
        } catch (Exception ex) {
            ligneVocale = null;
            showAlert(Alert.AlertType.ERROR, "Micro", "Enregistrement impossible : " + ex.getMessage());
        }
    }

    private void arreterEtEnvoyerVocal() {
        styleIconBtn(voiceBtn, "#25D366", "#128C7E");
        if (estEnGroupe && groupeActif != null) {
            chatStatus.setText(groupeActif.getNumerosMembres() != null
                    ? groupeActif.getNumerosMembres().size() + " membres" : "");
        } else if (numeroContactUtilisable(contactActif)) {
            chatStatus.setText("en ligne");
        }
        if (ligneVocale == null) return;
        TargetDataLine ligne = ligneVocale;
        ligneVocale = null;
        try {
            ligne.stop();
            ligne.close();
        } catch (Exception ignored) {}
        if (threadEnregistrement != null) {
            try { threadEnregistrement.join(800); } catch (InterruptedException ignored) {}
            threadEnregistrement = null;
        }
        if (bufferVocal == null || bufferVocal.size() < 200) {
            bufferVocal = null;
            showAlert(Alert.AlertType.WARNING, "Message vocal", "Enregistrement trop court. Maintenez le bouton 🎤 plus longtemps.");
            return;
        }
        try {
            AudioFormat format = formatVocalEnCours != null ? formatVocalEnCours
                    : new AudioFormat(16000, 16, 1, true, false);
            File wav = new File("downloads/vocal_" + System.currentTimeMillis() + ".wav");
            wav.getParentFile().mkdirs();
            byte[] pcm = bufferVocal.toByteArray();
            long frames = pcm.length / format.getFrameSize();
            try (AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(pcm), format, frames)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wav);
            }
            bufferVocal = null;
            envoyerFichierDepuisUI(wav, "audio");
        } catch (Exception e) {
            bufferVocal = null;
            showAlert(Alert.AlertType.ERROR, "Vocal", "Erreur enregistrement : " + e.getMessage());
        }
    }

    private static String formatTaille(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " Ko";
        return String.format("%.1f Mo", bytes / (1024.0 * 1024));
    }

    private void ouvrirGroupe(Groupe g) {
        this.groupeActif = g; this.estEnGroupe = true; this.contactActif = null; this.idConversationActive = null;
        chatName.setText(g.getNomGroupe());
        int nb = g.getNumerosMembres() != null ? g.getNumerosMembres().size() : 0;
        chatStatus.setText(nb + " membres");
        mettreAJourAvatar(chatAvatar, String.valueOf(g.getNomGroupe().charAt(0)).toUpperCase(), "#25D366");
        messagesBox.getChildren().clear();
        msgField.setDisable(false); majEtatBoutonEnvoi();
        btnAppelAudio.setVisible(true); btnAppelAudio.setManaged(true);
        btnAppelVideo.setVisible(true); btnAppelVideo.setManaged(true);
        // [NOUVEAU] Lier les boutons aux méthodes d'appel groupe
        btnAppelAudio.setOnAction(e -> demarrerAppelAudioGroupe(g));
        btnAppelVideo.setOnAction(e -> demarrerAppelVideoGroupe(g));
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

        // [NOUVEAU] Réinitialiser les actions pour les appels individuels
        btnAppelAudio.setOnAction(e -> demarrerAppelAudio(contactActif, chatName.getText()));
        btnAppelVideo.setOnAction(e -> demarrerAppelVideo(contactActif, chatName.getText()));
        groupMenu.setVisible(false); groupMenu.setManaged(false);
        if (id != -1) ClientHandlerAuth.getInstance().demanderMessages(id);
    }

    private void majEtatBoutonEnvoi() {
        boolean ok = estEnGroupe ? (groupeActif != null) : numeroContactUtilisable(contactActif);
        if (sendBtn != null) sendBtn.setDisable(!ok);
        if (attachBtn != null) attachBtn.setDisable(!ok);
        if (voiceBtn != null) voiceBtn.setDisable(!ok);
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
            if (FileMediaUtil.isGroupFileContent(m.getContenu())) {
                boolean groupeOuvert = estEnGroupe && groupeActif != null
                        && m.getIdGroupe() == groupeActif.getIdGroupe();
                if (!groupeOuvert && m.getIdGroupe() > 0) {
                    String[] meta = FileMediaUtil.parseGroupFileContent(m.getContenu());
                    String nomFichier = meta != null ? meta[1] : "fichier";
                    mettreAJourBadgeGroupeNonLu(m.getIdGroupe(), nomFichier);
                }
                ClientHandlerAuth.getInstance().demanderListeGroupes();
                return;
            }
            if (estEnGroupe && groupeActif != null && m.getIdGroupe() == groupeActif.getIdGroupe()) {
                Utilisateur moi = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
                boolean estMoi = moi != null && m.getTelephoneExpediteur().equals(moi.getNumeroTelephone());
                String time = m.getDateEnvoi() != null
                        ? m.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"))
                        : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                if (estMoi) {
                    messagesBox.getChildren().add(Messagefx.Messageenvoyer(m.getContenu(), time));
                } else {
                    messagesBox.getChildren().add(Messagefx.Messagerecu(m.getNomExpediteur() + ": " + m.getContenu(), time));
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
            groupesList.getItems().clear();
            groupesListMaster.clear();
            cacheGroupes.clear();
            if (gs == null) return;
            for (Groupe g : gs) {
                cacheGroupes.put(g.getIdGroupe(), g);
            }
            if (invitationAppelIdGroupe >= 0) {
                int id = invitationAppelIdGroupe;
                String type = invitationAppelType;
                String nom = invitationAppelNomGroupe.isEmpty() ? "Groupe" : invitationAppelNomGroupe;
                invitationAppelIdGroupe = -1;
                invitationAppelType = "";
                invitationAppelNomGroupe = "";
                proposerAppelGroupeEntrant(id, nom, type);
            }
            if (estEnGroupe && groupeActif != null) {
                for (Groupe g : gs) {
                    if (g.getIdGroupe() == groupeActif.getIdGroupe()) {
                        groupeActif.setNumerosMembres(g.getNumerosMembres() != null ? new ArrayList<>(g.getNumerosMembres()) : new ArrayList<>());
                        int nb = groupeActif.getNumerosMembres() != null ? groupeActif.getNumerosMembres().size() : 0;
                        chatStatus.setText(nb + " membres");
                        break;
                    }
                }
            }
            for (Groupe g : gs) {
                HBox item = makeConvItem(g.getNomGroupe(), "", "Groupe", "#25D366", g.getIdGroupe(), 0);
                item.setUserData(g);
                groupesListMaster.add(item);
            }
            filtrerListes();
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
            convListMaster.clear();
            if (cs == null) { filtrerListes(); return; }
            for (Conversation nouvelle : cs) {
                String color = "#128C7E";
                HBox item = makeConvItem(
                        nouvelle.getNomContact(),
                        nouvelle.getNumeroContact(),
                        nouvelle.getDernierMessage() != null ? nouvelle.getDernierMessage() : "",
                        color,
                        nouvelle.getIdConversation(),
                        nouvelle.getMessagesNonLus());
                convListMaster.add(item);
            }
            filtrerListes();
        });
    }

    @Override public void messagesRecus(List<Message> ms) {
        Platform.runLater(() -> {
            messagesBox.getChildren().clear();
            if (ms == null) return;
            for (Message m : ms) {
                String time = m.getDateEnvoi() != null ? m.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
                String type = m.getTypeMessage() != null ? m.getTypeMessage() : "texte";
                if (!"texte".equals(type) && m.getUrlFichier() != null && !m.getUrlFichier().isBlank()) {
                    File local = null;
                    try {
                        local = sauvegarderFichierLocal(m.getNomFichier(), Base64.getDecoder().decode(m.getUrlFichier()), null);
                    } catch (Exception e) {
                        System.out.println("[FILE] Historique 1:1 : " + e.getMessage());
                    }
                    ajouterBulleFichier(m.isEstMoi(), type, m.getNomFichier(), time, local);
                } else if (!"texte".equals(type) && m.getNomFichier() != null) {
                    ajouterBulleFichier(m.isEstMoi(), type, m.getNomFichier(), time, null);
                } else {
                    messagesBox.getChildren().add(m.isEstMoi()
                            ? Messagefx.Messageenvoyer(m.getContenuTexte(), time)
                            : Messagefx.Messagerecu(m.getContenuTexte(), time));
                }
            }
            if (!numeroContactUtilisable(contactActif)) contactActif = infererNumeroInterlocuteur(ms);
            scrollToBottom();
        });
    }

    @Override public void fichierRecu(String tel, String type, String name, String b64) {
        Platform.runLater(() -> traiterFichierRecu(tel, type, name, b64, false, null, null, null));
    }

    @Override
    public void fichierGroupeRecu(int idGroupe, String tel, String nomExp, String type, String name, String b64) {
        fichierGroupeRecu(idGroupe, tel, nomExp, type, name, b64, null);
    }

    @Override
    public void fichierGroupeRecu(int idGroupe, String tel, String nomExp, String type, String name,
                                  String b64, String dateEnvoi) {
        Platform.runLater(() -> traiterFichierRecu(tel, type, name, b64, true, idGroupe, nomExp,
                parserHeureMessage(dateEnvoi)));
    }

    private static LocalTime parserHeureMessage(String dateEnvoi) {
        if (dateEnvoi == null || dateEnvoi.isBlank()) return null;
        try {
            return java.time.LocalDateTime.parse(dateEnvoi).toLocalTime();
        } catch (Exception e) {
            try {
                return LocalTime.parse(dateEnvoi);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Override
    public void debutHistoriqueGroupe(int idGroupe) {
        Platform.runLater(() -> {
            if (estEnGroupe && groupeActif != null && groupeActif.getIdGroupe() == idGroupe) {
                messagesBox.getChildren().clear();
            }
        });
    }

    @Override
    public void finHistoriqueGroupe(int idGroupe) {
        Platform.runLater(() -> {
            if (estEnGroupe && groupeActif != null && groupeActif.getIdGroupe() == idGroupe) {
                scrollToBottom();
            }
        });
    }

    @Override public void fichierProgress(int percent) {
        Platform.runLater(() -> {
            if (fileProgressBar == null) return;
            fileProgressBar.setVisible(true);
            fileProgressBar.setManaged(true);
            fileProgressBar.setProgress(Math.min(1.0, percent / 100.0));
            if (percent >= 100) {
                fileProgressBar.setVisible(false);
                fileProgressBar.setManaged(false);
            }
        });
    }

    @Override public void fichierEnvoiReussi(String fileName) {
        Platform.runLater(() -> {
            if (fileProgressBar != null) {
                fileProgressBar.setProgress(1);
                fileProgressBar.setVisible(false);
                fileProgressBar.setManaged(false);
            }
        });
    }

    @Override public void fichierEnvoiEchoue(String raison) {
        Platform.runLater(() -> {
            if (fileProgressBar != null) {
                fileProgressBar.setVisible(false);
                fileProgressBar.setManaged(false);
            }
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Envoi fichier");
            a.setHeaderText(null);
            a.setContentText(raison != null ? raison : "Échec de l'envoi");
            ButtonType retry = new ButtonType("Réessayer");
            a.getButtonTypes().setAll(retry, ButtonType.CANCEL);
            a.showAndWait().ifPresent(btn -> {
                if (btn == retry) ClientHandlerAuth.getInstance().retryEnvoiFichier();
            });
        });
    }

    @Override
    public void appelGroupeEntrant(int idGroupe, String nomGroupe, String type, String initiateurNom) {
        Platform.runLater(() -> proposerAppelGroupeEntrant(idGroupe, nomGroupe, type));
    }

    /** Propose de rejoindre un appel groupe (sauf si nous l'avons initié ou sommes déjà dedans). */
    private void proposerAppelGroupeEntrant(int idGroupe, String nomGroupe, String type) {
        if (appelsGroupeInitiateur.contains(idGroupe)) return;
        if (estDejaDansAppelGroupe(idGroupe)) return;

        Groupe g = trouverGroupeParId(idGroupe);
        if (g == null) {
            invitationAppelIdGroupe = idGroupe;
            invitationAppelType = type;
            invitationAppelNomGroupe = nomGroupe;
            ClientHandlerAuth.getInstance().demanderListeGroupes();
            return;
        }

        String typeLabel = "VIDEO".equalsIgnoreCase(type) ? "vidéo" : "audio";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Appel " + typeLabel + " groupe");
        alert.setHeaderText(null);
        alert.setContentText("Un appel " + typeLabel + " est en cours dans \"" + nomGroupe + "\".\nVoulez-vous rejoindre ?");

        ButtonType accepter = new ButtonType("Rejoindre");
        ButtonType refuser = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accepter, refuser);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == accepter) {
            rejoindreAppelGroupe(g, type);
        }
    }

    @Override
    public void appelGroupeDemarre(int idGroupe, String type) {
        Platform.runLater(() -> {
            System.out.println("Appel groupe démarré: " + idGroupe + " type: " + type);
        });
    }


    @Override
    public void membreRejointAppelGroupe(int idGroupe, String numeroMembre, String nomMembre, String ip, String type, int port, int portAudio, boolean isReply) {
        Platform.runLater(() -> {
            if (estMonNumero(numeroMembre)) {
                System.out.println("[Appel] Ignoré : notification de notre propre participation");
                return;
            }

            int portAudioEffectif = portAudio > 0 ? portAudio : ("VIDEO".equalsIgnoreCase(type) ? port + 1 : port);

            // --- Cas 1 : on est déjà dans un appel audio pour ce groupe ---
            if (appelAudioGroupeActif != null && appelAudioGroupeActif.getIdGroupe() == idGroupe) {
                appelAudioGroupeActif.notifierMembreRejoint(numeroMembre, nomMembre, ip, portAudioEffectif);
                if (!isReply) {
                    int monPort = appelAudioGroupeActif.getLocalPort();
                    ClientHandlerAuth.getInstance().demarrerAppelGroupe(idGroupe, "AUDIO", monPort, monPort, true);
                }
                return;
            }

            // --- Cas 2 : on est déjà dans un appel vidéo pour ce groupe ---
            if (appelVideoGroupeActif != null && appelVideoGroupeActif.getIdGroupe() == idGroupe) {
                appelVideoGroupeActif.surParticipantRejoint(numeroMembre, nomMembre, ip, port, portAudioEffectif);
                if (!isReply) {
                    ClientHandlerAuth.getInstance().demarrerAppelGroupe(
                            idGroupe, "VIDEO",
                            appelVideoGroupeActif.getLocalPort(),
                            appelVideoGroupeActif.getLocalAudioPort(),
                            true);
                }
                return;
            }

            // --- Cas 3 : invitation entrante (pas encore dans l'appel) ---
            if (!isReply && !appelsGroupeInitiateur.contains(idGroupe)) {
                Groupe g = trouverGroupeParId(idGroupe);
                String nomGroupe = g != null ? g.getNomGroupe() : "Groupe";
                appelGroupeEntrant(idGroupe, nomGroupe, type, nomMembre);
            }
        });
    }

    @Override
    public void membreQuitteAppelGroupe(int idGroupe, String numeroMembre) {
        Platform.runLater(() -> {
            System.out.println(numeroMembre + " a quitté l'appel groupe " + idGroupe);
            if (appelAudioGroupeActif != null) {
                appelAudioGroupeActif.notifierMembreParti(numeroMembre);
            }
            if (appelVideoGroupeActif != null) {
                appelVideoGroupeActif.surParticipantParti(numeroMembre);
            }
        });
    }

    @Override
    public void appelGroupeTermine(int idGroupe) {
        Platform.runLater(() -> {
            // Fermer les fenetres d'appel si elles sont encore ouvertes
            if (appelAudioGroupeActif != null && appelAudioGroupeActif.getIdGroupe() == idGroupe) {
                appelAudioGroupeActif.fermerFenetre();
                // fermerFenetre() appelle raccrocherTout() qui déclenche onTermine → met à null
            }
            if (appelVideoGroupeActif != null && appelVideoGroupeActif.getIdGroupe() == idGroupe) {
                appelVideoGroupeActif.fermerFenetre();
            }
            // Sécurité : forcer la remise à null même si onTermine n'a pas été appelé
            appelAudioGroupeActif = null;
            appelVideoGroupeActif = null;
            appelsGroupeInitiateur.remove(idGroupe);
            arreterIndicateurAppelGroupe();
            showAlert(Alert.AlertType.INFORMATION, "Appel groupe", "L'appel est terminé.");
        });
    }
    @Override
    public void signalisationAppelGroupe(int idGroupe, String numeroSource, String typeSignal, String payload) {
        Platform.runLater(() -> {
            System.out.println("Signalisation appel groupe: " + idGroupe + " type: " + typeSignal);
        });
    }

    // removed duplicate fluxVideoGroupeRecu with javax.swing.text.html.ImageView

    @Override
    public void fluxVideoGroupeArrete(int idGroupe, String numeroExpediteur) {
        Platform.runLater(() -> {
            if (appelVideoGroupeActif != null) {
                appelVideoGroupeActif.surParticipantParti(numeroExpediteur);
            }
        });
    }
    @Override
    public void fluxVideoGroupeRecu(int idGroupe, String numeroExpediteur, ImageView videoNode) {
        // Not used anymore. GroupVideoUDP uses a direct callback to AppelVideoGroupe
    }

    @Override public void appelEntrant(String num, String type, String ip, String name) {
        Platform.runLater(() -> {
            // L'appelant sortant ne doit pas recevoir une popup "appel entrant"
            if (ClientHandlerAuth.getInstance().isEnAppel() && !ClientHandlerAuth.getInstance().isAppelEntrant()) {
                return;
            }
            if ("VIDEO".equalsIgnoreCase(type)) {
                typeAppelEnCours = "VIDEO";
                Appelvideo.recevoirAppel(primaryStage, name, num, ip);
            } else {
                typeAppelEnCours = "AUDIO";
                afficherFenetreAppel(name, false, num, ip);
            }
        });
    }

    @Override public void appelAccepte(String num, String ip) {
        Platform.runLater(() -> {
            if ("VIDEO".equalsIgnoreCase(typeAppelEnCours)) {
                if (stageAppel != null) stageAppel.close();
                Appelvideo.demarrer(primaryStage, chatName.getText(), contactActif, idConversationActive != null ? idConversationActive : -1, ip);
            } else if (statutAppelLabel != null) {
                statutAppelLabel.setText("En communication...");
            }
        });
    }

    @Override public void appelRefuse() {
        Platform.runLater(() -> {
            terminerAppelLocal(false);
            showAlert(Alert.AlertType.INFORMATION, "Appel", "Refusé");
        });
    }

    @Override public void appelTermine(String n) {
        Platform.runLater(() -> terminerAppelLocal(false));
    }

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

    private void demarrerAppelAudioGroupe(Groupe groupe) {
        demarrerAppelAudioGroupe(groupe, true);
    }

    private void demarrerAppelAudioGroupe(Groupe groupe, boolean commeInitiateur) {
        if (groupe == null) return;
        if (appelAudioGroupeActif != null || appelVideoGroupeActif != null) {
            showAlert(Alert.AlertType.WARNING, "Appel", "Vous êtes déjà dans un appel.");
            return;
        }
        int idGroupe = groupe.getIdGroupe();
        if (commeInitiateur) appelsGroupeInitiateur.add(idGroupe);
        appelAudioGroupeActif = AppelAudioGroupe.demarrer(primaryStage, groupe, idGroupe, () -> {
            appelAudioGroupeActif = null;
            appelsGroupeInitiateur.remove(idGroupe);
            arreterIndicateurAppelGroupe();
        });
        int portAudio = appelAudioGroupeActif.getLocalPort();
        ClientHandlerAuth.getInstance().demarrerAppelGroupe(idGroupe, "AUDIO", portAudio, portAudio, false);
        demarrerIndicateurAppelGroupe(idGroupe, groupe.getNomGroupe(), "AUDIO");
    }

    private void demarrerAppelVideoGroupe(Groupe groupe) {
        demarrerAppelVideoGroupe(groupe, true);
    }

    private void demarrerAppelVideoGroupe(Groupe groupe, boolean commeInitiateur) {
        if (groupe == null) return;
        if (appelAudioGroupeActif != null || appelVideoGroupeActif != null) {
            showAlert(Alert.AlertType.WARNING, "Appel", "Vous êtes déjà dans un appel.");
            return;
        }
        int idGroupe = groupe.getIdGroupe();
        if (commeInitiateur) appelsGroupeInitiateur.add(idGroupe);
        appelVideoGroupeActif = AppelVideoGroupe.demarrer(primaryStage, groupe, idGroupe, () -> {
            appelVideoGroupeActif = null;
            appelsGroupeInitiateur.remove(idGroupe);
            arreterIndicateurAppelGroupe();
        });
        ClientHandlerAuth.getInstance().demarrerAppelGroupe(
                idGroupe, "VIDEO",
                appelVideoGroupeActif.getLocalPort(),
                appelVideoGroupeActif.getLocalAudioPort(),
                false);
        demarrerIndicateurAppelGroupe(idGroupe, groupe.getNomGroupe(), "VIDEO");
    }

    /** Rejoint un appel groupe : ouvre directement l'écran d'appel (sans ouvrir le chat). */
    private void rejoindreAppelGroupe(Groupe groupe, String type) {
        if (groupe == null) return;
        if ("VIDEO".equalsIgnoreCase(type)) {
            demarrerAppelVideoGroupe(groupe, false);
        } else {
            demarrerAppelAudioGroupe(groupe, false);
        }
    }

    private Groupe trouverGroupeParId(int idGroupe) {
        Groupe g = cacheGroupes.get(idGroupe);
        if (g != null) return g;
        for (HBox item : groupesList.getItems()) {
            if (item.getUserData() instanceof Groupe gr && gr.getIdGroupe() == idGroupe) {
                cacheGroupes.put(idGroupe, gr);
                return gr;
            }
        }
        return null;
    }

    private boolean estDejaDansAppelGroupe(int idGroupe) {
        return (appelAudioGroupeActif != null && appelAudioGroupeActif.getIdGroupe() == idGroupe)
                || (appelVideoGroupeActif != null && appelVideoGroupeActif.getIdGroupe() == idGroupe);
    }

    private void demarrerIndicateurAppelGroupe(int idGroupe, String nomGroupe, String type) {
        idGroupeEnAppel = idGroupe;
        nomGroupeEnAppel = nomGroupe != null ? nomGroupe : "Groupe";
        typeAppelGroupeEnCours = type;
        debutAppelGroupe = Instant.now();

        if (barreAppelSidebar != null) {
            barreAppelSidebar.setVisible(true);
            barreAppelSidebar.setManaged(true);
        }
        if (appelEnCoursLabel != null) {
            appelEnCoursLabel.setVisible(true);
        }
        majAffichageDureeAppel();

        if (chronometreAppel != null) chronometreAppel.stop();
        chronometreAppel = new Timeline(new KeyFrame(Duration.seconds(1), e -> majAffichageDureeAppel()));
        chronometreAppel.setCycleCount(Timeline.INDEFINITE);
        chronometreAppel.play();
    }

    private void arreterIndicateurAppelGroupe() {
        if (chronometreAppel != null) {
            chronometreAppel.stop();
            chronometreAppel = null;
        }
        idGroupeEnAppel = -1;
        nomGroupeEnAppel = "";
        typeAppelGroupeEnCours = "";
        if (barreAppelSidebar != null) {
            barreAppelSidebar.setVisible(false);
            barreAppelSidebar.setManaged(false);
        }
        if (appelEnCoursLabel != null) {
            appelEnCoursLabel.setVisible(false);
        }
    }

    private void majAffichageDureeAppel() {
        if (debutAppelGroupe == null) return;
        long sec = java.time.Duration.between(debutAppelGroupe, Instant.now()).getSeconds();
        String duree = formatDureeAppel(sec);
        String typeLabel = "VIDEO".equalsIgnoreCase(typeAppelGroupeEnCours) ? "vidéo" : "audio";
        String texte = "🔴 " + nomGroupeEnAppel + " — Appel " + typeLabel + " " + duree;

        if (labelBarreAppelSidebar != null) labelBarreAppelSidebar.setText(texte);
        if (appelEnCoursLabel != null) appelEnCoursLabel.setText(duree);
    }

    private static String formatDureeAppel(long secondes) {
        long h = secondes / 3600;
        long m = (secondes % 3600) / 60;
        long s = secondes % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    /** Ramène la fenêtre d'appel au premier plan (bouton sur la barre latérale). */
    private void afficherFenetreAppelGroupeActif() {
        if (appelVideoGroupeActif != null) {
            appelVideoGroupeActif.afficherFenetre();
        } else if (appelAudioGroupeActif != null) {
            appelAudioGroupeActif.afficherFenetre();
        }
    }

    private void afficherFenetreAttenteVideo(String nom) {
        stageAppel = new Stage();
        VBox root = new VBox(20, makeAvatar(String.valueOf(nom.charAt(0)), "#25D366"), new Label(nom), new Label("Appel vidéo..."));
        root.setAlignment(Pos.CENTER); root.setPadding(new Insets(20));
        Button hangup = makeBtnAppel("📵", "#EA2424");
        hangup.setOnAction(e -> terminerAppelLocal(true));
        root.getChildren().add(hangup);
        stageAppel.setScene(new Scene(root, 300, 350)); stageAppel.show();
    }

    private void afficherFenetreAppel(String nom, boolean sortant, String num, String ip) {
        stageAppel = new Stage();
        VBox root = new VBox(20, makeAvatar(String.valueOf(nom.charAt(0)), "#25D366"), new Label(nom), statutAppelLabel = new Label(sortant ? "Appel..." : "Appel entrant..."));
        root.setAlignment(Pos.CENTER); root.setPadding(new Insets(20));
        if (sortant) {
            Button h = makeBtnAppel("📵", "#EA2424");
            h.setOnAction(e -> terminerAppelLocal(true));
            root.getChildren().add(h);
        } else {
            Button acc = makeBtnAppel("📞", "#25D366");
            acc.setOnAction(e -> {
                ClientHandlerAuth.getInstance().accepterAppel();
                statutAppelLabel.setText("En communication...");
            });
            Button ref = makeBtnAppel("📵", "#EA2424");
            ref.setOnAction(e -> {
                ClientHandlerAuth.getInstance().refuserAppel();
                terminerAppelLocal(false);
            });
            root.getChildren().add(new HBox(20, acc, ref));
        }
        stageAppel.setScene(new Scene(root, 300, 350)); stageAppel.show();
    }

    /** Normalise et compare un numéro avec l'utilisateur connecté. */
    private boolean estMonNumero(String numero) {
        if (numero == null || numero.isBlank()) return false;
        Utilisateur moi = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
        if (moi == null || moi.getNumeroTelephone() == null) return false;
        String monNum = moi.getNumeroTelephone().replaceAll("[^0-9]", "");
        String numDistant = numero.replaceAll("[^0-9]", "");
        if (monNum.isEmpty() || numDistant.isEmpty()) return false;
        if (monNum.equals(numDistant)) return true;
        int len = Math.min(monNum.length(), numDistant.length());
        return len >= 9 && monNum.regionMatches(monNum.length() - len, numDistant, numDistant.length() - len, len);
    }

    /** Arrête le micro local et notifie le serveur si nécessaire. */
    private void terminerAppelLocal(boolean notifierServeur) {
        arreterMediaLocal();
        if (notifierServeur) {
            ClientHandlerAuth.getInstance().raccrocher();
        }
        if (stageAppel != null) {
            stageAppel.close();
            stageAppel = null;
        }
        typeAppelEnCours = null;
    }

    private void arreterMediaLocal() {
        if (audioUDP != null) {
            audioUDP.arreter();
            audioUDP = null;
        }
        if (videoUDP != null) {
            videoUDP.arreter();
            videoUDP = null;
        }
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
        if (voiceBtn != null) voiceBtn.setDisable(true);
    }

    private void mettreAJourBadgeGroupeNonLu(int idGroupe, String fileName) {
        for (HBox item : groupesList.getItems()) {
            Object ud = item.getUserData();
            if (!(ud instanceof Groupe g) || g.getIdGroupe() != idGroupe) continue;
            item.getChildren().stream()
                    .filter(n -> n instanceof VBox)
                    .map(n -> (VBox) n)
                    .findFirst()
                    .ifPresent(vbox -> {
                        if (vbox.getChildren().size() >= 2) {
                            Label lbl = (Label) vbox.getChildren().get(1);
                            String hint = fileName != null && !fileName.isBlank()
                                    ? "• Fichier : " + fileName : "• Nouveau fichier";
                            lbl.setText(hint);
                            lbl.setTextFill(Color.web("#25D366"));
                        }
                    });
            break;
        }
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
        List<Contact> res = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (HBox item : convList.getItems()) {
            if (item.getUserData() == null) continue;
            String[] p = item.getUserData().toString().split(";");
            if (p.length >= 3 && seen.add(p[1])) {
                Contact c = new Contact(); c.setNumeroTelephone(p[1]); c.setNomComplet(p[2]); res.add(c);
            }
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

    private void scrollToBottom() { Platform.runLater(() -> scrollPane.setVvalue(1.0)); }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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

    @Override public void connexionReussie(Utilisateur u) {
        this.utilisateurConnecte = u;
        ClientHandlerAuth.getInstance().demanderListeGroupes();
    }
    @Override public void inscriptionReussie(String m) {}
    @Override public void erreur(String m) { Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", m)); }
    @Override public void deconnexion() { Platform.runLater(() -> showAlert(Alert.AlertType.WARNING, "Déconnexion", "Déconnecté")); }
    @Override public void contactAjoute(Contact c) {
        Platform.runLater(() -> {
            if (c == null || !numeroContactUtilisable(c.getNumeroTelephone())) return;
            String tel = c.getNumeroTelephone().trim();
            String nom = c.getNomComplet() != null && !c.getNomComplet().isBlank() ? c.getNomComplet().trim() : tel;
            boolean existe = false;
            for (HBox row : convList.getItems()) {
                Object ud = row.getUserData();
                if (ud instanceof String s) {
                    String[] p = s.split(";", 3);
                    if (p.length >= 2 && tel.equals(p[1])) {
                        existe = true;
                        break;
                    }
                }
            }
            if (!existe) {
                String[] colors = {"#25D366", "#128C7E", "#075E54", "#34B7F1"};
                String color = colors[(int) (Math.random() * colors.length)];
                convList.getItems().add(makeConvItem(nom, tel, "Nouveau contact", color, -1, 0));
            }
            assurerContactDansMesContacts(c, tel);
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    ClientHandlerAuth.getInstance().demanderConversations();
                    ClientHandlerAuth.getInstance().demanderContacts();
                });
            }).start();
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
            if (r.get() == accepter)  {
                ClientHandlerAuth.getInstance().accepterDemandeContact(numeroDemandeur);
                ClientHandlerAuth.getInstance().demanderConversations();
            }
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
