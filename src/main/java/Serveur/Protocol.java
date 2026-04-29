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
    // Appels
    CALL_REQUEST,
    CALL_ACCEPT ,
    CALL_REFUSE ,
    CALL_CANCEL , // ← pour distinguer annulation et fin d'appel
    CALL_END ,

    CONVERSATIONS_LIST,
    CONVERSATIONS_RECUES,
    MESSAGES_NON_LUS,


    AJOUTER_CONTACT,
    SUPPRIMER_CONTACT,
    LISTE_CONTACTS,
    REPONSE_LISTE_CONTACTS,
    CONTACT_AJOUTE,
    CONTACT_INEXISTANT,
    }