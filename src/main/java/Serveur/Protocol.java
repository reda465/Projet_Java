package Serveur;

public enum Protocol {
    // Auth
    LOGIN ,
    REGISTER,
    REGISTER_OK ,
    REGISTER_FAIL ,
    LOGIN_OK ,
    LOGIN_FAIL ,
    LOGOUT ,

    // Utilisateurs
    USERS_LIST,

    // Messagerie
    MSG_SEND ,
    MSG_RECEIVE ,

    //conversation
    GET_CONVERSATIONS,   // client demande sa liste de conversations
    CONVERSATIONS_LIST,
    CONVERSATIONS_RECUES,// serveur répond avec la liste
    GET_MESSAGES,        // client demande les messages d'une conversation
    MESSAGES_LIST,

    // Appels
    CALL_REQUEST,
    CALL_ACCEPT ,
    CALL_REFUSE ,
    CALL_END,

    //fichiers
    FILE_SEND,
    FILE_RECEIVE,
    FILE_FAIL
}