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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GroupVideoUDP {
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int LARGEUR_FRAME  = 320;
    private static final int HAUTEUR_FRAME  = 240;
    private static final int QUALITE_JPEG   = 30;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private boolean actif = false;
    private VideoCapture camera;
    private int localPort = -1;

    private ConcurrentHashMap<String, InetSocketAddress> destinations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> addressToNumero = new ConcurrentHashMap<>();
    private BiConsumer<String, Image> frameReceivedCallback;
    private Consumer<Image> localFrameCallback;

    static {
        OpenCV.loadLocally();
    }

    public GroupVideoUDP(BiConsumer<String, Image> frameReceivedCallback, Consumer<Image> localFrameCallback) {
        this.frameReceivedCallback = frameReceivedCallback;
        this.localFrameCallback = localFrameCallback;
    }

    private VideoCapture ouvrirCamera() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        int[] backends = isWindows
                ? new int[] { Videoio.CAP_DSHOW, Videoio.CAP_MSMF, Videoio.CAP_ANY }
                : new int[] { Videoio.CAP_ANY };

        for (int backend : backends) {
            for (int index = 0; index <= 2; index++) {
                VideoCapture tentative = new VideoCapture(index, backend);
                if (tentative.isOpened()) {
                    return tentative;
                }
                tentative.release();
            }
        }
        return null;
    }

    public int demarrer() {
        try {
            socketEnvoi = new DatagramSocket();
            socketReception = new DatagramSocket(0);
            localPort = socketReception.getLocalPort();
            actif = true;

            new Thread(() -> {
                try {
                    camera = ouvrirCamera();
                    if (camera == null || !camera.isOpened()) {
                        System.out.println("[GroupVideoUDP] Impossible d'ouvrir la caméra.");
                        return;
                    }
                    camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  LARGEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_FRAME);
                    camera.set(Videoio.CAP_PROP_FPS, 15);

                    Mat frame = new Mat();
                    Mat frameReduit = new Mat();
                    MatOfByte mob = new MatOfByte();
                    MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG);
                    int consecutiveGrabFailures = 0;

                    while (actif) {
                        boolean lectureOk = camera.read(frame);
                        if (!lectureOk || frame.empty()) {
                            consecutiveGrabFailures++;
                            if (consecutiveGrabFailures == 40) {
                                try { camera.release(); } catch (Exception ignored) {}
                                camera = ouvrirCamera();
                                if (camera != null && camera.isOpened()) {
                                    camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  LARGEUR_FRAME);
                                    camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_FRAME);
                                    camera.set(Videoio.CAP_PROP_FPS, 15);
                                    consecutiveGrabFailures = 0;
                                } else {
                                    return;
                                }
                            }
                            Thread.sleep(50);
                            continue;
                        }
                        consecutiveGrabFailures = 0;

                        Imgproc.resize(frame, frameReduit, new Size(LARGEUR_FRAME, HAUTEUR_FRAME));
                        Imgcodecs.imencode(".jpg", frameReduit, mob, params);
                        byte[] data = mob.toArray();
                        
                        // Callback for local preview
                        if (localFrameCallback != null) {
                            Image localImage = new Image(new ByteArrayInputStream(data));
                            Platform.runLater(() -> localFrameCallback.accept(localImage));
                        }

                        if (data.length <= MAX_PACKET_SIZE) {
                            for (InetSocketAddress addr : destinations.values()) {
                                socketEnvoi.send(new DatagramPacket(data, data.length, addr.getAddress(), addr.getPort()));
                            }
                        }
                        Thread.sleep(66);
                    }
                } catch (Exception e) { if (actif) e.printStackTrace(); }
            }, "GroupVideoUDP-Envoi").start();

            new Thread(() -> {
                byte[] buffer = new byte[MAX_PACKET_SIZE];
                while (actif) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socketReception.receive(packet);
                        
                        String remoteKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                        String numeroExpediteur = addressToNumero.get(remoteKey);
                        
                        if (numeroExpediteur != null) {
                            byte[] imgData = new byte[packet.getLength()];
                            System.arraycopy(packet.getData(), 0, imgData, 0, packet.getLength());
                            Image image = new Image(new ByteArrayInputStream(imgData));
                            if (!image.isError() && frameReceivedCallback != null) {
                                Platform.runLater(() -> frameReceivedCallback.accept(numeroExpediteur, image));
                            }
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        continue;
                    } catch (Exception e) { if (actif) e.printStackTrace(); }
                }
            }, "GroupVideoUDP-Reception").start();

            System.out.println("[GroupVideoUDP] Démarré sur le port " + localPort);
            return localPort;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getLocalPort() {
        return localPort;
    }

    public void addDestination(String numero, String ip, int port) {
        InetSocketAddress addr = new InetSocketAddress(ip, port);
        destinations.put(numero, addr);
        addressToNumero.put(ip + ":" + port, numero);
    }

    public void removeDestination(String numero) {
        InetSocketAddress addr = destinations.remove(numero);
        if (addr != null) {
            addressToNumero.remove(addr.getAddress().getHostAddress() + ":" + addr.getPort());
        }
    }

    public void arreter() {
        actif = false;
        try { if (camera != null) camera.release(); } catch (Exception e) {}
        try { if (socketEnvoi != null) socketEnvoi.close(); } catch (Exception e) {}
        try { if (socketReception != null) socketReception.close(); } catch (Exception e) {}
        destinations.clear();
        addressToNumero.clear();
        System.out.println("[GroupVideoUDP] Arrêté");
    }
}
