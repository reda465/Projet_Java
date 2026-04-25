package Serveur;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread{
    Socket socket;
    ClientHandler(Socket s){
        socket=s;
    }
    @Override
    public void run() {
        try(
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw=new PrintWriter(socket.getOutputStream());

        ){
            while(true)
        }
        catch(IOException e){
            e.printStackTrace();
        }


    }
}
