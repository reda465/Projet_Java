package service;

import Serveur.Protocol;
import client.ClientReseau;
import client.FileUploadResult;
import network.Packet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;

/**
 * Envoi de fichiers par morceaux vers le serveur (1-à-1 ou groupe).
 */
public class FileTransferService {

    private static final int CHUNK = 36 * 1024;

    private final ClientReseau client;

    public FileTransferService(ClientReseau client) {
        this.client = client;
    }

    public static String detecterTypeMessage(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".webp"))
            return "image";
        if (n.endsWith(".mp4") || n.endsWith(".webm") || n.endsWith(".mov")) return "video";
        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg") || n.endsWith(".m4a")) return "audio";
        return "document";
    }

    public FileUploadResult envoyerVersContact(String numeroDest, File fichier, String typeMessage,
                                               String legende, DoubleConsumer progression) throws Exception {
        if (client == null || !client.isConnecte()) return FileUploadResult.fail("NON_CONNECTE");
        byte[] data = Files.readAllBytes(fichier.toPath());
        String session = UUID.randomUUID().toString().replace("-", "");
        String captionB64 = Base64.getEncoder().encodeToString((legende == null ? "" : legende).getBytes(StandardCharsets.UTF_8));
        String begin = session + "|IND|" + numeroDest + "|" + fichier.getName() + "|" + data.length + "|"
                + (typeMessage != null ? typeMessage : detecterTypeMessage(fichier)) + "|" + captionB64;
        CompletableFuture<FileUploadResult> attente = new CompletableFuture<>();
        client.enregistrerAttenteUpload(session, attente);
        client.envoyer(new Packet(Protocol.FILE_UPLOAD_BEGIN, begin));
        int sent = 0;
        while (sent < data.length) {
            int to = Math.min(sent + CHUNK, data.length);
            String b64 = Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(data, sent, to));
            client.envoyer(new Packet(Protocol.FILE_UPLOAD_CHUNK, session + "|" + b64));
            sent = to;
            if (progression != null) progression.accept((double) sent / (double) data.length);
        }
        client.envoyer(new Packet(Protocol.FILE_UPLOAD_END, session));
        return attente.get(10, TimeUnit.MINUTES);
    }

    public FileUploadResult envoyerVersGroupe(int idGroupe, File fichier, String typeMessage,
                                              String legende, DoubleConsumer progression) throws Exception {
        if (client == null || !client.isConnecte()) return FileUploadResult.fail("NON_CONNECTE");
        byte[] data = Files.readAllBytes(fichier.toPath());
        String session = UUID.randomUUID().toString().replace("-", "");
        String captionB64 = Base64.getEncoder().encodeToString((legende == null ? "" : legende).getBytes(StandardCharsets.UTF_8));
        String begin = session + "|GRP|" + idGroupe + "|" + fichier.getName() + "|" + data.length + "|"
                + (typeMessage != null ? typeMessage : detecterTypeMessage(fichier)) + "|" + captionB64;
        CompletableFuture<FileUploadResult> attente = new CompletableFuture<>();
        client.enregistrerAttenteUpload(session, attente);
        client.envoyer(new Packet(Protocol.FILE_UPLOAD_BEGIN, begin));
        int sent = 0;
        while (sent < data.length) {
            int to = Math.min(sent + CHUNK, data.length);
            String b64 = Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(data, sent, to));
            client.envoyer(new Packet(Protocol.FILE_UPLOAD_CHUNK, session + "|" + b64));
            sent = to;
            if (progression != null) progression.accept((double) sent / (double) data.length);
        }
        client.envoyer(new Packet(Protocol.FILE_UPLOAD_END, session));
        return attente.get(10, TimeUnit.MINUTES);
    }
}