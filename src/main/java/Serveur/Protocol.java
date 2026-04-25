package Serveur;

public class Protocol {
    // Auth
    public static final String LOGIN        = "LOGIN";
    public static final String REGISTER     = "REGISTER";
    public static final String LOGIN_OK     = "LOGIN_OK";
    public static final String LOGIN_FAIL   = "LOGIN_FAIL";
    public static final String LOGOUT       = "LOGOUT";

    // Utilisateurs
    public static final String USERS_LIST   = "USERS_LIST";

    // Messagerie
    public static final String MSG_SEND     = "MSG_SEND";
    public static final String MSG_RECEIVE  = "MSG_RECEIVE";

    // Appels
    public static final String CALL_REQUEST = "CALL_REQUEST";
    public static final String CALL_ACCEPT  = "CALL_ACCEPT";
    public static final String CALL_REFUSE  = "CALL_REFUSE";
    public static final String CALL_END     = "CALL_END";

    // Séparateur
    public static final String SEP = "|";
}