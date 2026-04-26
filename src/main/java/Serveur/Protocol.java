package Serveur;

public enum Protocol {

    // ==========================
    // AUTHENTIFICATION
    // ==========================
    LOGIN,
    REGISTER,
    LOGOUT,

    LOGIN_OK,
    LOGIN_FAIL,

    REGISTER_OK,
    REGISTER_FAIL,

    // ==========================
    // UTILISATEURS CONNECTÉS
    // ==========================
    USERS_LIST,

    // ==========================
    // MESSAGERIE
    // ==========================
    MSG_SEND,
    MSG_RECEIVE,

    // ==========================
    // APPELS (DEMANDE/REPONSE)
    // ==========================
    CALL_REQUEST,
    CALL_ACCEPT,
    CALL_REFUSE,
    CALL_END,

    // ==========================
    // DONNÉES AUDIO/VIDEO
    // ==========================
    AUDIO_DATA,
    VIDEO_FRAME,

    // ==========================
    // ERREUR GENERALE
    // ==========================
    ERROR
}