package client;

import javax.sound.sampled.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupAudioUDP {
    private static final int TAILLE_BUFFER = 4096;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean actif = false;
    private int localPort = -1;

    private ConcurrentHashMap<String, InetSocketAddress> destinations = new ConcurrentHashMap<>();

    public int demarrer() {
        try {
            socketEnvoi = new DatagramSocket();
            socketReception = new DatagramSocket(0); // Port aléatoire
            localPort = socketReception.getLocalPort();
            actif = true;

            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    microphone = (TargetDataLine) AudioSystem.getLine(info);
                    microphone.open(format);
                    microphone.start();
                    byte[] buffer = new byte[TAILLE_BUFFER];
                    while (actif) {
                        int lu = microphone.read(buffer, 0, buffer.length);
                        if (lu > 0) {
                            for (InetSocketAddress addr : destinations.values()) {
                                socketEnvoi.send(new DatagramPacket(buffer, lu, addr.getAddress(), addr.getPort()));
                            }
                        }
                    }
                } catch (Exception e) { if (actif) e.printStackTrace(); }
            }, "GroupAudioUDP-Micro").start();

            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    speakers = (SourceDataLine) AudioSystem.getLine(info);
                    speakers.open(format);
                    speakers.start();
                    byte[] buffer = new byte[TAILLE_BUFFER];
                    while (actif) {
                        DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                        socketReception.receive(p);
                        speakers.write(p.getData(), 0, p.getLength());
                    }
                } catch (Exception e) { if (actif) e.printStackTrace(); }
            }, "GroupAudioUDP-HautParleurs").start();

            System.out.println("[GroupAudioUDP] Démarré sur le port " + localPort);
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
        destinations.put(numero, new InetSocketAddress(ip, port));
    }

    public void removeDestination(String numero) {
        destinations.remove(numero);
    }

    public void arreter() {
        actif = false;
        try { if (microphone != null) { microphone.stop(); microphone.close(); } } catch (Exception e) {}
        try { if (speakers != null) { speakers.stop(); speakers.close(); } } catch (Exception e) {}
        try { if (socketEnvoi != null) socketEnvoi.close(); } catch (Exception e) {}
        try { if (socketReception != null) socketReception.close(); } catch (Exception e) {}
        destinations.clear();
        System.out.println("[GroupAudioUDP] Arrêté");
    }
}
