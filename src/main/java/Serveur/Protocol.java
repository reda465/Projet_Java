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
    // Contacts

    BLOCK_CONTACT,
    BLOCK_OK,

    // Notification de premier message
    CONTACT_REQUEST,    // ← notification envoyée au destinataire
    CONTACT_ACCEPTED,   // ← le destinataire accepte
    CONTACT_BLOCKED,    // ← le destinataire bloque
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

    ADD_CONTACT,
    ADD_CONTACT_OK,
    ADD_CONTACT_FAIL,
    CONTACTS_LIST,
    GET_CONTACTS,
    // Groupes V2
    CREATE_GROUP,
    CREATE_GROUP_OK,
    CREATE_GROUP_FAIL,
    GET_GROUPS,
    GROUPS_LIST,
    GET_GROUP_MESSAGES,
    GROUP_MESSAGES_LIST,
    SEND_GROUP_MESSAGE,
    GROUP_MESSAGE_RECEIVE,
    ADD_GROUP_MEMBER,
    ADD_GROUP_MEMBER_OK,
    ADD_GROUP_MEMBER_FAIL,
    REMOVE_GROUP_MEMBER,
    REMOVE_GROUP_MEMBER_OK,
    REMOVE_GROUP_MEMBER_FAIL,
    QUIT_GROUP,
    QUIT_GROUP_OK,
    DELETE_GROUP,
    DELETE_GROUP_OK,
    RENAME_GROUP,
    RENAME_GROUP_OK,
    START_GROUP_CALL,
    JOIN_GROUP_CALL,
    LEAVE_GROUP_CALL,
    END_GROUP_CALL,
    //fichier
    FILE_SEND,
    FILE_RECEIVE,
    FILE_FAIL

}