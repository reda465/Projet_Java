package network;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Packet {

    private Protocol protocol;
    private String data;

    public Packet(Protocol protocol, String contenu) {
        this.protocol = protocol;
        this.data = (contenu != null) ? contenu : "";
    }

    public String toString() {
        return protocol + "|" + data;
    }

    public static Packet fromString(String ligne) {
        if (ligne == null) {
            throw new IllegalArgumentException("ligne null");
        }
        ligne = ligne.trim();
        if (ligne.isEmpty()) {
            throw new IllegalArgumentException("ligne vide");
        }
        int sep = ligne.indexOf('|');
        String head = (sep < 0 ? ligne : ligne.substring(0, sep)).trim();
        String cont = sep < 0 ? "" : ligne.substring(sep + 1);
        Protocol prot = Protocol.valueOf(head);
        return new Packet(prot, cont);
    }

    public String toDebugString() {
        return "Packet[" + protocol + "', contenu='" + data + "']";
    }
}
