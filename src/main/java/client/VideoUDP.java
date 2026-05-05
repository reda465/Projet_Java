package client;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import java.io.ByteArrayInputStream;
import java.net.*;
public class VideoUDP {
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int LARGEUR_FRAME  = 320;
    private static final int HAUTEUR_FRAME  = 240;
    private static final int QUALITE_JPEG   = 30;
    private static final int TIMEOUT_RECEPTION_MS = 500;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private boolean actif = false;
    private VideoCapture camera;
    private Thread threadEnvoi;
    private Thread threadReception;
    private ImageView videoView;

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
            socketReception.setSoTimeout(TIMEOUT_RECEPTION_MS);

            actif = true;

            // ================= THREAD ENVOI =================
            threadEnvoi = new Thread(() -> {
                try {
                    camera = new VideoCapture(0);

                    if (!camera.isOpened()) {
                        System.out.println("❌ Impossible d'ouvrir la caméra !");
                        return;
                    }
                    camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  LARGEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FPS, 15);

                    Mat frame = new Mat();
                    Mat frameReduit = new Mat();
                    MatOfByte mob = new MatOfByte();
                    MatOfInt params = new MatOfInt(
                            Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG
                    );

                    while (actif) {
                        camera.read(frame);

                        if (frame.empty()) {
                            Thread.sleep(50);
                            continue;
                        }

                        Imgproc.resize(frame, frameReduit,
                                new Size(LARGEUR_FRAME, HAUTEUR_FRAME));

                        Imgcodecs.imencode(".jpg", frameReduit, mob, params);
                        byte[] data = mob.toArray();

                        if (data.length > MAX_PACKET_SIZE) {
                            System.out.println("[VideoUDP] ⚠️ Frame encore trop grande ("
                                    + data.length + " bytes), ignorée.");
                            Thread.sleep(50);
                            continue;
                        }

                        DatagramPacket packet =
                                new DatagramPacket(data, data.length, addr, portDistant);
                        socketEnvoi.send(packet);

                        Thread.sleep(66);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    if (actif) e.printStackTrace();
                }
            }, "VideoUDP-Envoi");
            threadEnvoi.setDaemon(true);
            threadEnvoi.start();

            // ================= THREAD RECEPTION =================
            threadReception = new Thread(() -> {
                try {
                    byte[] buffer = new byte[MAX_PACKET_SIZE];

                    while (actif) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socketReception.receive(packet);

                            byte[] imgData = new byte[packet.getLength()];
                            System.arraycopy(packet.getData(), 0, imgData, 0, packet.getLength());

                            Image image = new Image(new ByteArrayInputStream(imgData));
                            if (image.isError()) {
                                System.err.println("[VideoUDP] Image corrompue reçue");
                                continue;
                            }
                            if (videoView != null) {
                                Platform.runLater(() -> videoView.setImage(image));
                            }
                        } catch (java.net.SocketTimeoutException e) {
                            continue;
                        } catch (java.net.SocketException e) {
                            if (actif) e.printStackTrace();
                        } catch (Exception e) {
                            if (actif) e.printStackTrace();
                        }
                    }

                    System.out.println("[VideoUDP] Thread réception terminé.");

                } catch (Exception e) {
                    if (actif) e.printStackTrace();
                }
            }, "VideoUDP-Reception");
            threadReception.setDaemon(true);
            threadReception.start();

            System.out.println("[VideoUDP] Vidéo démarrée → envoi vers "
                    + ipDistant + ":" + portDistant
                    + " | écoute sur port " + monPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== STOP VIDEO =====================
    public void arreter() {
        actif = false;

        try {
            if (camera != null) {
                camera.release();
                camera = null;
            }
        } catch (Exception ignored) {}

        try {
            if (socketEnvoi != null && !socketEnvoi.isClosed()) {
                socketEnvoi.close();
            }
        } catch (Exception ignored) {}

        try {
            if (socketReception != null && !socketReception.isClosed()) {
                socketReception.close();
            }
        } catch (Exception ignored) {}

        try {
            if (threadEnvoi != null) {
                threadEnvoi.interrupt();
                threadEnvoi.join(1000);
            }
        } catch (Exception ignored) {}

        try {
            if (threadReception != null) {
                threadReception.interrupt();
                threadReception.join(1000);
            }
        } catch (Exception ignored) {}

        System.out.println("[VideoUDP] Vidéo arrêtée proprement.");
    }
    public boolean isActif() {
        return actif;
    }
}