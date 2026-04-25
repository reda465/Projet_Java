package service;

import client.ClientReseauCALL;
import network.Commande;
import network.Packet;
import javax.sound.sampled.*;
public class CallAudiooService {

        private ClientReseauCALL clientReseau;

        // AUDIO CONFIG
        private AudioFormat format;
        private TargetDataLine microphone;
        private SourceDataLine speakers;

        private Thread captureThread;
        private boolean enAppel = false;

        public CallAudiooService(ClientReseauCALL clientReseau) {
            this.clientReseau = clientReseau;

            // format audio standard (IMPORTANT client/serveur identique)
            format = new AudioFormat(16000, 16, 1, true, false);
        }

        // =====================================================
        // 1. DEMARRER APPEL AUDIO
        // =====================================================
        public void startCall(String caller, String receiver) {
            enAppel = true;

            Packet packet = new Packet(
                    Commande.Debuter_AUDIO_CALL,
                    caller + ";" + receiver
            );

            clientReseau.envoyer(packet);

            demarrerMicro();
            demarrerLectureAudio();

            System.out.println("[AUDIO] Appel démarré");
        }

        // =====================================================
        // 2. TERMINER APPEL AUDIO
        // =====================================================
        public void stopCall(String caller, String receiver) {
            enAppel = false;

            Packet packet = new Packet(
                    Commande.Arreter_AUDIO_CALL,
                    caller + ";" + receiver
            );

            clientReseau.envoyer(packet);

            stopMicro();
            stopAudio();

            System.out.println("[AUDIO] Appel terminé");
        }

        // =====================================================
        // 3. MICRO (CAPTURE + ENVOI)
        // =====================================================
        private void demarrerMicro() {

            captureThread = new Thread(() -> {

                try {
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                    microphone = (TargetDataLine) AudioSystem.getLine(info);
                    microphone.open(format);
                    microphone.start();

                    System.out.println("[MIC] Micro ouvert");

                    byte[] buffer = new byte[4096];

                    while (enAppel) {

                        int bytesRead = microphone.read(buffer, 0, buffer.length);

                        if (bytesRead > 0) {

                            envoyerAudio(buffer);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            captureThread.start();
        }

        // ENVOI AUDIO
        public void envoyerAudio(byte[] data) {
            Packet packet = new Packet(Commande.Data_AUDIO, data);
            clientReseau.envoyer(packet);
        }

        // =====================================================
        // 4. LECTURE AUDIO (RECEPTION)
        // =====================================================
        private void demarrerLectureAudio() {

            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();

                System.out.println("[SPEAKER] Haut-parleur prêt");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // méthode appelée depuis ClientReseau quand AUDIO_DATA arrive
        public void jouerAudio(byte[] data) {

            if (speakers != null) {
                speakers.write(data, 0, data.length);
            }
        }

        // =====================================================
        // 5. STOP MICRO
        // =====================================================
        private void stopMicro() {

            try {
                if (microphone != null) {
                    microphone.stop();
                    microphone.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // =====================================================
        // 6. STOP AUDIO
        // =====================================================
        private void stopAudio() {

            try {
                if (speakers != null) {
                    speakers.stop();
                    speakers.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}

