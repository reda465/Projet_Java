/*package service;

import model.CallVideo;
import client.ClientReseauCALL;
import network.Commande;
import network.Packet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CallVideoService {

    private ClientReseauCALL clientReseau;

    // État de l'appel
    private boolean enAppel = false;

    // Thread de capture webcam
    private Thread captureThread;

    // Fenêtre d'affichage vidéo
    private JFrame fenetre;
    private JLabel affichageLabel;

    // =====================================================
    // CONSTRUCTEUR
    // =====================================================
    public CallVideoService(ClientReseauCALL clientReseau) {
        this.clientReseau = clientReseau;
    }

    // =====================================================
    // 1. DEMARRER APPEL VIDEO
    // =====================================================
    public void startCall(CallVideo call) {
        call.demarrer();
        enAppel = true;

        Packet packet = new Packet(
                Commande.Debuter_Video_CALL,
                call.getCaller().getNom() + ";" + call.getReceiver().getNom()
        );

        clientReseau.envoyer(packet);

        ouvrirFenetre(call);
        demarrerCapture();

        System.out.println("[VIDEO] Appel vidéo démarré.");
    }

    // =====================================================
    // 2. TERMINER APPEL VIDEO
    // =====================================================
    public void stopCall(CallVideo call) {
        enAppel = false;
        call.terminer();

        Packet packet = new Packet(
                Commande.Arreter_Video_CALL,
                call.getCaller().getNom() + ";" + call.getReceiver().getNom()
        );

        clientReseau.envoyer(packet);

        stopCapture();
        fermerFenetre();

        System.out.println("[VIDEO] Appel vidéo terminé.");
    }

    // =====================================================
    // 3. ENVOYER UNE FRAME
    // =====================================================
    public void sendFrame(byte[] frameData) {
        Packet packet = new Packet(Commande.Data_Appel_Video, frameData);
        clientReseau.envoyer(packet);
    }

    // =====================================================
    // 4. RECEVOIR ET AFFICHER UNE FRAME
    //    Appelée depuis ClientReseau quand Data_Appel_Video arrive
    // =====================================================
    public void afficherFrame(byte[] frameData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(frameData));
            if (img != null && affichageLabel != null) {
                ImageIcon icon = new ImageIcon(img);
                SwingUtilities.invokeLater(() -> affichageLabel.setIcon(icon));
            }
        } catch (Exception e) {
            System.out.println("[VIDEO] Erreur affichage frame : " + e.getMessage());
        }
    }

    // =====================================================
    // 5. CAPTURE WEBCAM (Robot = capture écran comme simulation)
    // =====================================================
    private void demarrerCapture() {

        captureThread = new Thread(() -> {
            try {
                Robot robot = new Robot();
                Rectangle zone = new Rectangle(0, 0, 640, 480);

                System.out.println("[CAM] Capture démarrée");

                while (enAppel) {

                    // Capture une frame de l'écran (simule la webcam)
                    BufferedImage frame = robot.createScreenCapture(zone);

                    // Redimensionner pour alléger (320x240)
                    BufferedImage petite = redimensionner(frame, 320, 240);

                    // Convertir en bytes JPEG
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(petite, "jpg", baos);
                    byte[] frameBytes = baos.toByteArray();

                    // Envoyer via le réseau (ou loopback en test)
                    sendFrame(frameBytes);

                    Thread.sleep(33); // ~30 fps
                }

            } catch (Exception e) {
                System.out.println("[CAM] Erreur capture : " + e.getMessage());
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    // =====================================================
    // 6. STOP CAPTURE
    // =====================================================
    private void stopCapture() {
        enAppel = false;
        if (captureThread != null) {
            captureThread.interrupt();
        }
        System.out.println("[CAM] Capture arrêtée");
    }

    // =====================================================
    // 7. FENETRE D'AFFICHAGE
    // =====================================================
    private void ouvrirFenetre(CallVideo call) {
        SwingUtilities.invokeLater(() -> {
            fenetre = new JFrame("📹 Appel Vidéo : "
                    + call.getCaller().getNom() + " → " + call.getReceiver().getNom());
            fenetre.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            fenetre.setSize(340, 290);

            affichageLabel = new JLabel("En attente de la webcam...", JLabel.CENTER);
            fenetre.add(affichageLabel, BorderLayout.CENTER);
            fenetre.setVisible(true);
        });
    }

    private void fermerFenetre() {
        if (fenetre != null) {
            SwingUtilities.invokeLater(() -> fenetre.dispose());
        }
    }

    // =====================================================
    // UTILITAIRE : redimensionner une image
    // =====================================================
    private BufferedImage redimensionner(BufferedImage src, int largeur, int hauteur) {
        BufferedImage result = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(src, 0, 0, largeur, hauteur, null);
        g.dispose();
        return result;
    }
}