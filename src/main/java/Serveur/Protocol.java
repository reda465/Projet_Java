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

    CREATE_GROUP,           // Client demande création groupe
    CREATE_GROUP_OK,        // Serveur confirme création
    CREATE_GROUP_FAIL,      // Échec (nom existant, erreur, etc.)

    ADD_MEMBER,             // Ajouter membre au groupe
    ADD_MEMBER_OK,
    ADD_MEMBER_FAIL,

    REMOVE_MEMBER,          // Retirer membre (admin seulement)
    REMOVE_MEMBER_OK,
    REMOVE_MEMBER_FAIL,

    LEAVE_GROUP,            // Quitter le groupe soi-même
    LEAVE_GROUP_OK,

    DELETE_GROUP,           // Supprimer groupe (créateur seulement)
    DELETE_GROUP_OK,
    DELETE_GROUP_FAIL,

    GET_GROUPS,             // Demander liste des groupes du client
    GROUPS_LIST,            // Serveur envoie la liste

    GET_GROUP_MEMBERS,      // Demander membres d'un groupe
    GROUP_MEMBERS_LIST,     // Serveur envoie les membres

    GROUP_MSG_SEND,         // Envoyer message dans groupe
    GROUP_MSG_RECEIVE,      // Recevoir message de groupe

    UPDATE_GROUP_NAME,      // Modifier nom du groupe (admin)
    UPDATE_GROUP_NAME_OK,

    //fichier
    FILE_SEND,
    FILE_RECEIVE,
    FILE_FAIL
}