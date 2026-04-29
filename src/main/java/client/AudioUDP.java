package client;

import javax.sound.sampled.*;
import java.net.*;

public class AudioUDP {
    private static final int TAILLE_BUFFER = 4096;

    private DatagramSocket socketEnvoi;
    private DatagramSocket socketReception;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean actif = false;

    public AudioUDP() {}

    public void demarrer(String ipDistant, int portDistant, int monPort) {
        try {
            InetAddress addr = InetAddress.getByName(ipDistant);
            socketEnvoi = new DatagramSocket();
            socketReception = new DatagramSocket(monPort);
            actif = true;

            // Micro
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
                            socketEnvoi.send(new DatagramPacket(buffer, lu, addr, portDistant));
                        }
                    }
                } catch (Exception e) { if (actif) e.printStackTrace(); }
            }).start();

            // Haut-parleurs
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
            }).start();

            System.out.println("[UDP] Audio démarré sur port " + monPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void arreter() {
        actif = false;
        try {
            if (microphone != null) { microphone.stop(); microphone.close(); }
            if (speakers != null) { speakers.stop(); speakers.close(); }
            if (socketEnvoi != null) socketEnvoi.close();
            if (socketReception != null) socketReception.close();
        } catch (Exception e) {}
        System.out.println("[UDP] Audio arrêté");
    }
}