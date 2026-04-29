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
        String[] parts = ligne.split("\\|", 2);

        Protocol prot = Protocol.valueOf(parts[0]);
        String cont = parts[1];

        return new Packet(prot, cont);
    }

    public String toDebugString() {
        return "Packet[" + protocol + "', contenu='" + data + "']";
    }
}
