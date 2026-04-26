package Serveur;

import network.Packet;
import Serveur.Protocol;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {

    // Stocker les clients connectés : userId -> PrintWriter
    private final ConcurrentHashMap<Integer, PrintWriter> clients = new ConcurrentHashMap<>();

    // =====================================================
    // Ajouter un client connecté
    // =====================================================
    public void registerClient(int userId, PrintWriter writer) {
        clients.put(userId, writer);
        System.out.println("[CALL_MANAGER] Client enregistré : " + userId);
    }

    // =====================================================
    // Supprimer un client
    // =====================================================
    public void removeClient(int userId) {
        clients.remove(userId);
        System.out.println("[CALL_MANAGER] Client supprimé : " + userId);
    }

    // =====================================================
    // Envoyer un packet à un utilisateur
    // =====================================================
    private void envoyerA(int destinataireId, Packet packet) {
        PrintWriter writer = clients.get(destinataireId);

        if (writer != null) {
            writer.println(packet.toString());
        } else {
            System.out.println("[CALL_MANAGER] Destinataire introuvable : " + destinataireId);
        }
    }

    // =====================================================
    // CALL REQUEST
    // data = "type;fromId;toId"
    // =====================================================
    public void callRequest(String data) {
        // Exemple data: "audio;1;2" ou "video;1;2"
        String[] parts = data.split(";");

        String type = parts[0];
        int fromId = Integer.parseInt(parts[1]);
        int toId = Integer.parseInt(parts[2]);

        Packet p = new Packet(Protocol.CALL_REQUEST, type + ";" + fromId + ";" + toId);
        p.setExpediteurId(fromId);

        envoyerA(toId, p);

        System.out.println("[CALL_MANAGER] CALL_REQUEST envoyé de " + fromId + " vers " + toId);
    }

    // =====================================================
    // CALL ACCEPT
    // data = "type;fromId;toId"
    // =====================================================
    public void callAccept(String data) {
        String[] parts = data.split(";");

        String type = parts[0];
        int fromId = Integer.parseInt(parts[1]);
        int toId = Integer.parseInt(parts[2]);

        Packet p = new Packet(Protocol.CALL_ACCEPT, type + ";" + fromId + ";" + toId);
        p.setExpediteurId(toId);

        envoyerA(fromId, p);

        System.out.println("[CALL_MANAGER] CALL_ACCEPT envoyé de " + toId + " vers " + fromId);
    }

    // =====================================================
    // CALL REFUSE
    // data = "type;fromId;toId"
    // =====================================================
    public void callRefuse(String data) {
        String[] parts = data.split(";");

        String type = parts[0];
        int fromId = Integer.parseInt(parts[1]);
        int toId = Integer.parseInt(parts[2]);

        Packet p = new Packet(Protocol.CALL_REFUSE, type + ";" + fromId + ";" + toId);
        p.setExpediteurId(toId);

        envoyerA(fromId, p);

        System.out.println("[CALL_MANAGER] CALL_REFUSE envoyé de " + toId + " vers " + fromId);
    }

    // =====================================================
    // CALL END
    // data = "type;fromId;toId"
    // =====================================================
    public void callEnd(String data) {
        String[] parts = data.split(";");

        String type = parts[0];
        int fromId = Integer.parseInt(parts[1]);
        int toId = Integer.parseInt(parts[2]);

        Packet p = new Packet(Protocol.CALL_END, type + ";" + fromId + ";" + toId);
        p.setExpediteurId(fromId);

        envoyerA(toId, p);

        System.out.println("[CALL_MANAGER] CALL_END envoyé de " + fromId + " vers " + toId);
    }

    // =====================================================
    // AUDIO DATA
    // =====================================================
    public void transferAudio(int fromId, int toId, byte[] audioData) {
        Packet p = new Packet(Protocol.AUDIO_DATA, audioData);
        p.setExpediteurId(fromId);

        envoyerA(toId, p);
    }

    // =====================================================
    // VIDEO FRAME
    // =====================================================
    public void transferVideo(int fromId, int toId, byte[] frameData) {
        Packet p = new Packet(Protocol.VIDEO_FRAME, frameData);
        p.setExpediteurId(fromId);

        envoyerA(toId, p);
    }
}