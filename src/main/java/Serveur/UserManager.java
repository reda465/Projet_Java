package Serveur;

import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private static UserManager instance;
    private final ConcurrentHashMap<String, ClientHandler> connectedUsers
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> ips = new ConcurrentHashMap<>();

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void addUser(String telephone, ClientHandler handler) {
        connectedUsers.put(telephone, handler);
        String ip = handler.getSocket().getInetAddress().getHostAddress();
        ips.put(telephone, ip);
    }

    public void removeUser(String telephone) {
        connectedUsers.remove(telephone);
        ips.remove(telephone);
    }

    public ClientHandler getHandler(String telephone) {
        return connectedUsers.get(telephone);
    }

    public String getIP(String telephone) {
        return ips.get(telephone);
    }
    public boolean isOnline(String telephone) {
        return connectedUsers.containsKey(telephone);
    }

    public String getOnlineUsersList() {
        return String.join(",", connectedUsers.keySet());
    }

    public void broadcast(String message) {
        connectedUsers.values().forEach(h -> h.sendMessage(message));
    }
}
