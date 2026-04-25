package Serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurMT extends Thread{
    public static void main(String[] args) {
        new ServeurMT().start();
    }

    @Override
    public void run() {
        try (
            ServerSocket ss = new ServerSocket(5000);

        )
        {
           while(true){
               Socket s= ss.accept();
               System.out.println("Le client connecté : "+s.getRemoteSocketAddress());
               new ClientHandler(s).start();
           }
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }
}
