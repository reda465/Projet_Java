package client;

import javax.sound.sampled.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UDP audio pour les appels de groupe.
 * Chaque paquet est préfixé : [1 octet longueur numéro][numéro UTF-8][PCM]
 * Cela permet d'identifier l'expéditeur sans ambiguïté.
 */
public class GroupAudioUDP {
    private static final int TAILLE_BUFFER = 4096;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean actif = false;
    private volatile boolean microActif = true;
    private int localPort = -1;
    private String monNumero = "";

    private final ConcurrentHashMap<String, InetSocketAddress> destinations = new ConcurrentHashMap<>();

    public void setMonNumero(String numero) {
        this.monNumero = numero != null ? numero : "";
    }

    public void setMicroActif(boolean actif) {
        this.microActif = actif;
    }

    public int demarrer() {
        return demarrerSurPort(0); // Port aléatoire
    }

    /** Démarre en écoutant sur un port précis (utile pour videoPort+1 dans les appels vidéo). */
    public int demarrerSurPort(int portSpecifique) {
        try {
            socketReception = new DatagramSocket(portSpecifique);
            socketEnvoi     = new DatagramSocket();
            localPort = socketReception.getLocalPort();
            actif = true;

            System.out.println("[GroupAudioUDP] Tentative démarrage sur port " + portSpecifique + " (réel: " + localPort + ")");

            // ── Thread envoi (micro → destinations) ────────────────────────
            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    if (!AudioSystem.isLineSupported(info)) {
                        System.err.println("[GroupAudioUDP] Format micro non supporté !");
                    }
                    microphone = (TargetDataLine) AudioSystem.getLine(info);
                    microphone.open(format);
                    microphone.start();
                    System.out.println("[GroupAudioUDP] Microphone démarré.");

                    byte[] numBytes = monNumero.getBytes(StandardCharsets.UTF_8);
                    int    numLen   = Math.min(numBytes.length, 255);
                    byte[] pcm      = new byte[TAILLE_BUFFER];

                    while (actif) {
                        int lu = microphone.read(pcm, 0, pcm.length);
                        if (lu > 0 && microActif && !destinations.isEmpty()) {
                            // Construire payload [numLen][numéro][PCM]
                            byte[] payload = new byte[1 + numLen + lu];
                            payload[0] = (byte) numLen;
                            System.arraycopy(numBytes, 0, payload, 1, numLen);
                            System.arraycopy(pcm,      0, payload, 1 + numLen, lu);
                            
                            for (InetSocketAddress addr : destinations.values()) {
                                try {
                                    socketEnvoi.send(new DatagramPacket(payload, payload.length,
                                            addr.getAddress(), addr.getPort()));
                                } catch (Exception ex) { /* ignore single destination error */ }
                            }
                        }
                    }
                } catch (Exception e) { 
                    System.err.println("[GroupAudioUDP] Erreur Thread Envoi: " + e.getMessage());
                    if (actif) e.printStackTrace(); 
                }
            }, "GroupAudioUDP-Micro").start();

            // ── Thread réception (paquets distants → haut-parleurs) ─────────
            new Thread(() -> {
                try {
                    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    speakers = (SourceDataLine) AudioSystem.getLine(info);
                    speakers.open(format);
                    speakers.start();
                    System.out.println("[GroupAudioUDP] Haut-parleurs démarrés.");

                    byte[] buf = new byte[1 + 255 + TAILLE_BUFFER];
                    String monNumNormalise = monNumero.replaceAll("[^0-9]", "");

                    while (actif) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        socketReception.receive(p);
                        int len = p.getLength();
                        if (len < 2) continue;

                        int numLen = buf[0] & 0xFF;
                        if (1 + numLen >= len) continue;

                        String expediteur = new String(buf, 1, numLen, StandardCharsets.UTF_8).trim();
                        
                        // Normalisation pour comparaison robuste
                        String expNormalise = expediteur.replaceAll("[^0-9]", "");
                        if (!monNumNormalise.isEmpty() && expNormalise.equals(monNumNormalise)) {
                            continue;
                        }

                        int pcmStart = 1 + numLen;
                        int pcmLen   = len - pcmStart;
                        speakers.write(buf, pcmStart, pcmLen);
                    }
                } catch (Exception e) { 
                    System.err.println("[GroupAudioUDP] Erreur Thread Réception: " + e.getMessage());
                    if (actif) e.printStackTrace(); 
                }
            }, "GroupAudioUDP-HautParleurs").start();

            System.out.println("[GroupAudioUDP] Entièrement démarré — port réception " + localPort);
            return localPort;
        } catch (Exception e) {
            System.err.println("[GroupAudioUDP] Échec démarrage: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public int getLocalPort() { return localPort; }

    public void addDestination(String numero, String ip, int port) {
        destinations.put(numero, new InetSocketAddress(ip, port));
        System.out.println("[GroupAudioUDP] Destination : " + numero + " → " + ip + ":" + port);
    }

    public void removeDestination(String numero) {
        destinations.remove(numero);
    }

    public void arreter() {
        actif = false;
        try { if (microphone != null) { microphone.stop(); microphone.close(); } } catch (Exception ignored) {}
        try { if (speakers   != null) { speakers.stop();   speakers.close();   } } catch (Exception ignored) {}
        try { if (socketEnvoi    != null) socketEnvoi.close();    } catch (Exception ignored) {}
        try { if (socketReception != null) socketReception.close(); } catch (Exception ignored) {}
        destinations.clear();
        System.out.println("[GroupAudioUDP] Arrêté");
    }
}
