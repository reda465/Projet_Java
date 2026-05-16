package service;

import Serveur.Protocol;
import client.ClientReseau;
import network.Packet;
import util.FileMediaUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;

public class Fileservice {

    private final ClientReseau clientReseau;
    private volatile PendingUpload dernierEnvoi;

    public Fileservice(ClientReseau clientReseau) {
        this.clientReseau = clientReseau;
    }

    public void envoyerFichier(String telDest, File file) {
        envoyerFichier(telDest, file, null);
    }

    public void envoyerFichier(String telDest, File file, Consumer<Integer> onProgress) {
        if (file == null || !file.isFile()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String type = FileMediaUtil.detectType(file);
            envoyerFichierBytes(telDest, type, file.getName(), bytes, onProgress, false, -1);
        } catch (Exception e) {
            e.printStackTrace();
            notifierErreur(onProgress, "Lecture fichier impossible : " + e.getMessage());
        }
    }

    public void envoyerFichierGroupe(int idGroupe, File file, Consumer<Integer> onProgress) {
        if (file == null || !file.isFile()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String type = FileMediaUtil.detectType(file);
            envoyerFichierBytes(String.valueOf(idGroupe), type, file.getName(), bytes, onProgress, true, idGroupe);
        } catch (Exception e) {
            e.printStackTrace();
            notifierErreur(onProgress, "Lecture fichier impossible : " + e.getMessage());
        }
    }

    public void retryDernierEnvoi() {
        PendingUpload p = dernierEnvoi;
        if (p == null) return;
        envoyerFichierBytes(p.dest, p.type, p.fileName, p.bytes, p.onProgress, p.groupe, p.idGroupe);
    }

    private void envoyerFichierBytes(String dest, String type, String fileName, byte[] bytes,
                                     Consumer<Integer> onProgress, boolean groupe, int idGroupe) {
        dernierEnvoi = new PendingUpload(dest, type, fileName, bytes, onProgress, groupe, idGroupe);
        int chunkSize = FileMediaUtil.CHUNK_SIZE;
        int total = (int) Math.ceil(bytes.length / (double) chunkSize);
        if (total <= 0) total = 1;

        if (total == 1) {
            String b64 = Base64.getEncoder().encodeToString(bytes);
            String data;
            if (groupe) {
                data = idGroupe + "|" + type + "|" + fileName + "|" + b64;
                clientReseau.envoyer(new Packet(Protocol.FILE_GROUP_SEND, data));
            } else {
                data = dest + "|" + type + "|" + fileName + "|" + b64;
                clientReseau.envoyer(new Packet(Protocol.FILE_SEND, data));
            }
            if (onProgress != null) onProgress.accept(50);
            return;
        }

        for (int i = 0; i < total; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, bytes.length);
            byte[] slice = new byte[end - start];
            System.arraycopy(bytes, start, slice, 0, slice.length);
            String b64 = Base64.getEncoder().encodeToString(slice);
            String data;
            if (groupe) {
                data = idGroupe + "|" + type + "|" + fileName + "|" + total + "|" + i + "|" + b64;
                clientReseau.envoyer(new Packet(Protocol.FILE_GROUP_SEND, data));
            } else {
                data = dest + "|" + type + "|" + fileName + "|" + total + "|" + i + "|" + b64;
                clientReseau.envoyer(new Packet(Protocol.FILE_SEND, data));
            }
            if (onProgress != null) {
                onProgress.accept((int) ((i + 1) * 80L / total));
            }
        }
    }

    private void notifierErreur(Consumer<Integer> onProgress, String msg) {
        System.out.println("[FILE] " + msg);
        clientReseau.notifierEchecFichier(msg);
    }

    private static final class PendingUpload {
        final String dest;
        final String type;
        final String fileName;
        final byte[] bytes;
        final Consumer<Integer> onProgress;
        final boolean groupe;
        final int idGroupe;

        PendingUpload(String dest, String type, String fileName, byte[] bytes,
                      Consumer<Integer> onProgress, boolean groupe, int idGroupe) {
            this.dest = dest;
            this.type = type;
            this.fileName = fileName;
            this.bytes = bytes;
            this.onProgress = onProgress;
            this.groupe = groupe;
            this.idGroupe = idGroupe;
        }
    }
}
