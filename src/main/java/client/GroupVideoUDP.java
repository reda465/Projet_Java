package client;

import javafx.application.Platform;
import javafx.scene.image.Image;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * UDP vidéo pour les appels de groupe.
 * Chaque paquet est préfixé par un en-tête :
 *   [1 octet = longueur du numéro][numéro en UTF-8][données JPEG]
 * Cela permet d'identifier l'expéditeur sans ambiguïté même quand
 * plusieurs clients tournent sur 127.0.0.1.
 */
public class GroupVideoUDP {
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int LARGEUR_FRAME   = 320;
    private static final int HAUTEUR_FRAME   = 240;
    private static final int QUALITE_JPEG    = 30;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private boolean actif = false;
    private VideoCapture camera;
    private int localPort = -1;

    /** Mon propre numéro de téléphone, inclus dans chaque paquet envoyé. */
    private String monNumero = "";

    private final ConcurrentHashMap<String, InetSocketAddress> destinations = new ConcurrentHashMap<>();
    private final BiConsumer<String, Image> frameReceivedCallback;
    private final Consumer<Image>           localFrameCallback;

    static { OpenCV.loadLocally(); }

    public GroupVideoUDP(BiConsumer<String, Image> frameReceivedCallback,
                         Consumer<Image>           localFrameCallback) {
        this.frameReceivedCallback = frameReceivedCallback;
        this.localFrameCallback    = localFrameCallback;
    }

    /** Définir le numéro local avant de démarrer. */
    public void setMonNumero(String numero) {
        this.monNumero = numero != null ? numero : "";
    }

    private VideoCapture ouvrirCamera() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        int[] backends = isWindows
                ? new int[]{Videoio.CAP_DSHOW, Videoio.CAP_MSMF, Videoio.CAP_ANY}
                : new int[]{Videoio.CAP_ANY};
        for (int backend : backends) {
            for (int index = 0; index <= 2; index++) {
                VideoCapture vc = new VideoCapture(index, backend);
                if (vc.isOpened()) {
                    System.out.println("[GroupVideoUDP] Caméra ouverte backend=" + backend + " index=" + index);
                    return vc;
                }
                vc.release();
            }
        }
        return null;
    }

    public int demarrer() {
        try {
            socketReception = new DatagramSocket(0);
            socketEnvoi     = new DatagramSocket();
            localPort = socketReception.getLocalPort();
            actif = true;

            // ── Thread envoi ──────────────────────────────────────────────
            new Thread(() -> {
                try {
                    camera = ouvrirCamera();
                    if (camera == null) { System.out.println("[GroupVideoUDP] Pas de caméra."); return; }
                    camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  LARGEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FPS, 15);

                    Mat frame = new Mat(), frameR = new Mat();
                    MatOfByte mob = new MatOfByte();
                    MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG);
                    int fails = 0;

                    while (actif) {
                        if (!camera.read(frame) || frame.empty()) {
                            if (++fails == 40) {
                                camera.release();
                                camera = ouvrirCamera();
                                if (camera == null) return;
                                camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  LARGEUR_FRAME);
                                camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_FRAME);
                                camera.set(Videoio.CAP_PROP_FPS, 15);
                                fails = 0;
                            }
                            Thread.sleep(50); continue;
                        }
                        fails = 0;
                        Imgproc.resize(frame, frameR, new Size(LARGEUR_FRAME, HAUTEUR_FRAME));
                        Imgcodecs.imencode(".jpg", frameR, mob, params);
                        byte[] jpeg = mob.toArray();

                        // Prévisualisation locale
                        if (localFrameCallback != null) {
                            Image img = new Image(new ByteArrayInputStream(jpeg));
                            Platform.runLater(() -> localFrameCallback.accept(img));
                        }

                        // Construire paquet avec en-tête [len][numéro][jpeg]
                        byte[] numBytes = monNumero.getBytes(StandardCharsets.UTF_8);
                        int    numLen   = Math.min(numBytes.length, 255);
                        int    total    = 1 + numLen + jpeg.length;
                        if (total <= MAX_PACKET_SIZE) {
                            byte[] payload = new byte[total];
                            payload[0] = (byte) numLen;
                            System.arraycopy(numBytes, 0, payload, 1, numLen);
                            System.arraycopy(jpeg,     0, payload, 1 + numLen, jpeg.length);

                            for (InetSocketAddress addr : destinations.values()) {
                                socketEnvoi.send(new DatagramPacket(payload, total,
                                        addr.getAddress(), addr.getPort()));
                            }
                        }
                        Thread.sleep(66);
                    }
                } catch (Exception e) { if (actif) e.printStackTrace(); }
            }, "GroupVideoUDP-Envoi").start();

            // ── Thread réception ──────────────────────────────────────────
            new Thread(() -> {
                byte[] buf = new byte[MAX_PACKET_SIZE];
                while (actif) {
                    try {
                        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                        socketReception.receive(pkt);

                        int len = pkt.getLength();
                        if (len < 2) continue;

                        // Lire l'en-tête
                        int numLen = buf[0] & 0xFF;
                        if (1 + numLen >= len) continue;

                        String expediteur = new String(buf, 1, numLen, StandardCharsets.UTF_8).trim();
                        int    jpegStart  = 1 + numLen;
                        int    jpegLen    = len - jpegStart;

                        // Ignorer nos propres paquets (rebond rare mais possible)
                        if (expediteur.equals(monNumero.trim())) continue;

                        byte[] imgData = new byte[jpegLen];
                        System.arraycopy(buf, jpegStart, imgData, 0, jpegLen);
                        Image image = new Image(new ByteArrayInputStream(imgData));
                        if (!image.isError() && frameReceivedCallback != null) {
                            final String num = expediteur;
                            Platform.runLater(() -> frameReceivedCallback.accept(num, image));
                        }
                    } catch (SocketTimeoutException e) {
                        // normal
                    } catch (Exception e) { if (actif) e.printStackTrace(); }
                }
            }, "GroupVideoUDP-Reception").start();

            System.out.println("[GroupVideoUDP] Démarré — port réception " + localPort);
            return localPort;
        } catch (Exception e) { e.printStackTrace(); return -1; }
    }

    public int getLocalPort() { return localPort; }

    public void addDestination(String numero, String ip, int port) {
        destinations.put(numero, new InetSocketAddress(ip, port));
        System.out.println("[GroupVideoUDP] Destination : " + numero + " → " + ip + ":" + port);
    }

    public void removeDestination(String numero) {
        destinations.remove(numero);
    }

    public void arreter() {
        actif = false;
        try { if (camera != null) camera.release(); }         catch (Exception ignored) {}
        try { if (socketEnvoi    != null) socketEnvoi.close(); }    catch (Exception ignored) {}
        try { if (socketReception != null) socketReception.close(); } catch (Exception ignored) {}
        destinations.clear();
        System.out.println("[GroupVideoUDP] Arrêté");
    }
}
