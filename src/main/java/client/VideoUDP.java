package client;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VideoUDP {

    private static final int MAX_PACKET_SIZE = 60000;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;

    private boolean actif = false;

    private VideoCapture camera;

    private Thread threadEnvoi;
    private Thread threadReception;

    private ImageView videoView; // pour afficher la vidéo reçue

    static {
        OpenCV.loadLocally();
    }

    public VideoUDP() {}

    // ===================== LANCER VIDEO =====================
    public void demarrer(String ipDistant, int portDistant, int monPort, ImageView view) {
        try {
            this.videoView = view;

            InetAddress addr = InetAddress.getByName(ipDistant);

            socketEnvoi = new DatagramSocket();
            socketReception = new DatagramSocket(monPort);

            actif = true;

            // ================= THREAD ENVOI =================
            threadEnvoi = new Thread(() -> {
                try {
                    camera = new VideoCapture(0);

                    if (!camera.isOpened()) {
                        System.out.println("❌ Impossible d'ouvrir la caméra !");
                        return;
                    }

                    Mat frame = new Mat();

                    while (actif) {
                        camera.read(frame);

                        if (!frame.empty()) {
                            MatOfByte mob = new MatOfByte();
                            Imgcodecs.imencode(".jpg", frame, mob);

                            byte[] data = mob.toArray();

                            if (data.length > MAX_PACKET_SIZE) {
                                System.out.println("⚠️ Frame trop grande : " + data.length + " bytes");
                                continue;
                            }

                            DatagramPacket packet =
                                    new DatagramPacket(data, data.length, addr, portDistant);

                            socketEnvoi.send(packet);
                        }

                        Thread.sleep(50); // ~20 FPS
                    }

                } catch (Exception e) {
                    if (actif) e.printStackTrace();
                }
            });

            threadEnvoi.start();

            // ================= THREAD RECEPTION =================
            threadReception = new Thread(() -> {
                try {
                    byte[] buffer = new byte[MAX_PACKET_SIZE];

                    while (actif) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socketReception.receive(packet);

                        byte[] imgData = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, imgData, 0, packet.getLength());

                        Image image = new Image(new ByteArrayInputStream(imgData));

                        if (videoView != null) {
                            Platform.runLater(() -> videoView.setImage(image));
                        }
                    }

                } catch (Exception e) {
                    if (actif) e.printStackTrace();
                }
            });

            threadReception.start();

            System.out.println("[UDP] Vidéo démarrée sur port " + monPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== STOP VIDEO =====================
    public void arreter() {
        actif = false;

        try {
            if (camera != null) camera.release();
        } catch (Exception ignored) {}

        try {
            if (socketEnvoi != null) socketEnvoi.close();
        } catch (Exception ignored) {}

        try {
            if (socketReception != null) socketReception.close();
        } catch (Exception ignored) {}

        try {
            if (threadEnvoi != null) threadEnvoi.interrupt();
        } catch (Exception ignored) {}

        try {
            if (threadReception != null) threadReception.interrupt();
        } catch (Exception ignored) {}

        System.out.println("[UDP] Vidéo arrêtée");
    }
    public boolean isActif() {
        return actif;
    }
}

