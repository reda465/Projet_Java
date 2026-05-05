package com.ensa;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockFileServer {

    private static final int PORT = 9091;
    private static final ConcurrentHashMap<String, ClientSession> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("✅ MockFileServer démarré sur port " + PORT);

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

                        // FILE_SEND|telDest|fileName|base64
                        case "FILE_SEND":
                            if (parts.length < 4) break;

                            String telDest = parts[1];
                            String fileName = parts[2];
                            String base64 = parts[3];

                            ClientSession receiver = clients.get(telDest);

                            if (receiver == null) {
                                out.println("FILE_FAIL|DEST_OFFLINE");
                                System.out.println("❌ Destinataire hors ligne");
                            } else {
                                receiver.out.println("FILE_RECEIVE|" + telephone + "|" + fileName + "|" + base64);
                                System.out.println("📤 fichier transféré de " + telephone + " vers " + telDest);
                            }
                            break;

                        default:
                            out.println("UNKNOWN");
                            break;
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Déconnexion client : " + telephone);
            } finally {
                if (telephone != null) clients.remove(telephone);
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }
}