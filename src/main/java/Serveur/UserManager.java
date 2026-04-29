package Serveur;

import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static UserManager instance;
    private final ConcurrentHashMap<String, ClientHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> ips = new ConcurrentHashMap<>();

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void addUser(String telephone, ClientHandler handler) {
        handlers.put(telephone, handler);
        String ip = handler.getSocket().getInetAddress().getHostAddress();
        ips.put(telephone, ip);
    }

    public void removeUser(String telephone) {
        handlers.remove(telephone);
        ips.remove(telephone);
    }

    public ClientHandler getHandler(String telephone) {
        return handlers.get(telephone);
    }

    public String getIP(String telephone) {
        return ips.get(telephone);
    }

    public void broadcast(String message) {
        for (ClientHandler h : handlers.values()) h.sendMessage(message);
    }

    public String getOnlineUsersList() {
        return String.join(",", handlers.keySet());
    }
}