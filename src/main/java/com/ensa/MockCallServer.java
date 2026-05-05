package com.ensa;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockCallServer {

    private static final int PORT = 9090;

    // tel -> handler
    private static final ConcurrentHashMap<String, ClientSession> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("✅ MockCallServer démarré sur port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new ClientSession(socket).start();
        }
    }

    static class ClientSession extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String telephone;

        public ClientSession(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("[RECU] " + line);

                    String[] parts = line.split("\\|", -1);
                    String cmd = parts[0];

                    switch (cmd) {

                        // LOGIN|tel|pass
                        case "LOGIN":
                            telephone = parts[1];
                            clients.put(telephone, this);
                            out.println("LOGIN_OK|User_" + telephone + "|" + telephone);
                            System.out.println("✅ LOGIN OK : " + telephone);
                            break;

                        // CALL_REQUEST|dest|AUDIO
                        case "CALL_REQUEST":
                            String dest = parts[1];
                            String type = parts[2];

                            ClientSession receiver = clients.get(dest);
                            if (receiver == null) {
                                out.println("CALL_END|" + dest + "|HORS_LIGNE");
                                break;
                            }

                            String ipAppelant = socket.getInetAddress().getHostAddress();

                            // serveur envoie : CALL_REQUEST|tel|nom|type|idAppel|ipAppelant
                            receiver.out.println("CALL_REQUEST|" + telephone + "|User_" + telephone + "|" + type + "|1|" + ipAppelant);

                            System.out.println("📞 CALL_REQUEST envoyé à " + dest);
                            break;

                        // CALL_ACCEPT|telephoneAppelant
                        case "CALL_ACCEPT":
                            String telAppelant = parts[1];

                            ClientSession caller = clients.get(telAppelant);
                            if (caller != null) {
                                String ipAccepteur = socket.getInetAddress().getHostAddress();
                                caller.out.println("CALL_ACCEPT|" + telephone + "|" + ipAccepteur);
                                System.out.println("✅ CALL_ACCEPT envoyé à " + telAppelant);
                            }
                            break;

                        // CALL_REFUSE|telephoneAppelant
                        case "CALL_REFUSE":
                            String telAppelantRefuse = parts[1];

                            ClientSession callerRefuse = clients.get(telAppelantRefuse);
                            if (callerRefuse != null) {
                                callerRefuse.out.println("CALL_REFUSE|" + telephone);
                                System.out.println("❌ CALL_REFUSE envoyé à " + telAppelantRefuse);
                            }
                            break;

                        // CALL_END|telephoneDest
                        case "CALL_END":
                            String telDest = parts[1];
                            ClientSession other = clients.get(telDest);

                            if (other != null) {
                                other.out.println("CALL_END|" + telephone);
                                System.out.println("📴 CALL_END envoyé à " + telDest);
                            }
                            break;

                        default:
                            out.println("UNKNOWN");
                            break;
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Client déconnecté : " + telephone);
            } finally {
                if (telephone != null) clients.remove(telephone);
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}