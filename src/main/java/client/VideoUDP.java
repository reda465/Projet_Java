package client;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class VideoUDP {

    private static final int TAILLE_BUFFER = 65507; // Max UDP packet

    private DatagramSocket socket;
    private InetAddress ipDistant;
    private int portDistant;

    private Thread threadEnvoi;
    private Thread threadReception;
    private boolean actif = false;

    // Pour afficher la vidéo reçue
    private JLabel labelVideo; // Composant Swing/JavaFX pour afficher l'image

    public VideoUDP(JLabel labelVideo) {
        this.labelVideo = labelVideo;
    }

    public void demarrer(String ip, int port) {
        try {
            this.ipDistant = InetAddress.getByName(ip);
            this.portDistant = port;
            this.socket = new DatagramSocket();

            actif = true;
            demarrerCapture();
            demarrerAffichage();

            System.out.println("[VIDEO] Démarré avec " + ip + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void arreter() {
        actif = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
        System.out.println("[VIDEO] Arrêté");
    }

    // ==================== ENVOI (Webcam → UDP) ====================
    private void demarrerCapture() {
        threadEnvoi = new Thread(() -> {
            try {
                // Ouvrir la webcam avec OpenCV ou JavaCV
                // Exemple simplifié avec OpenCV :
                org.bytedeco.opencv.opencv_videoio.VideoCapture camera =
                        new org.bytedeco.opencv.opencv_videoio.VideoCapture(0);

                if (!camera.isOpened()) {
                    System.out.println("[VIDEO] Webcam non disponible");
                    return;
                }

                org.bytedeco.opencv.opencv_core.Mat frame = new org.bytedeco.opencv.opencv_core.Mat();

                while (actif) {
                    camera.read(frame);
                    if (frame.empty()) continue;

                    // Compresser en JPEG
                    byte[] imageBytes = compresserFrame(frame);

                    // Envoyer en UDP
                    DatagramPacket packet = new DatagramPacket(
                            imageBytes, imageBytes.length, ipDistant, portDistant);
                    socket.send(packet);

                    Thread.sleep(33); // ~30 FPS
                }

                camera.release();
            } catch (Exception e) { if (actif) e.printStackTrace(); }
        });
        threadEnvoi.start();
    }

    // ==================== RÉCEPTION (UDP → Écran) ====================
    private void demarrerAffichage() {
        threadReception = new Thread(() -> {
            try {
                byte[] buffer = new byte[TAILLE_BUFFER];

                while (actif) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Décompresser l'image
                    BufferedImage image = decompresserFrame(packet.getData(), packet.getLength());

                    // Afficher dans l'interface
                    if (image != null && labelVideo != null) {
                        SwingUtilities.invokeLater(() -> {
                            labelVideo.setIcon(new ImageIcon(image));
                            labelVideo.repaint();
                        });
                    }
                }
            } catch (Exception e) { if (actif) e.printStackTrace(); }
        });
        threadReception.start();
    }

    // ==================== COMPRESSION / DÉCOMPRESSION ====================
    private byte[] compresserFrame(org.bytedeco.opencv.opencv_core.Mat frame) throws IOException {
        // Convertir Mat → BufferedImage
        BufferedImage image = matToBufferedImage(frame);

        // Compresser en JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private BufferedImage decompresserFrame(byte[] data, int length) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length);
            return ImageIO.read(bais);
        } catch (Exception e) {
            return null;
        }
    }

    // Conversion Mat → BufferedImage
    private BufferedImage matToBufferedImage(org.bytedeco.opencv.opencv_core.Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        byte[] sourceData = new byte[width * height * channels];
        mat.data().get(sourceData);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, width, height, sourceData);

        return image;
    }
}