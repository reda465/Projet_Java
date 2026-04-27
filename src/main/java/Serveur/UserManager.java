package Serveur;

import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private static UserManager instance;
    private final ConcurrentHashMap<String, ClientHandler> connectedUsers
            = new ConcurrentHashMap<>();

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void addUser(String telephone, ClientHandler handler) {
        connectedUsers.put(telephone, handler);
    }

    public void removeUser(String telephone) {
        connectedUsers.remove(telephone);
    }

    public ClientHandler getHandler(String telephone) {
        return connectedUsers.get(telephone);
    }

    public boolean isOnline(String telephone) {
        return connectedUsers.containsKey(telephone);
    }

    public String getOnlineUsersList() {
        return String.join(",", connectedUsers.keySet());
    }

    public void broadcast(network.Packet packet) {
        connectedUsers.values().forEach(h -> h.sendMessage(packet));
    }
}
