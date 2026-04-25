package network;

// enum = ÉNUMÉRATION = liste de valeurs fixes
public enum Commande {
    // Authentification
    INSCRIPTION,      // "Je veux m'inscrire"
    CONNEXION,        // "Je veux me connecter"
    DECONNEXION,      // "Je veux partir"

    // Réponses du serveur
    SUCCES,           // "C'est bon !"
    ERREUR,           // "Problème !"

    // Messages
    ENVOI_MESSAGE,    // "J'envoie un message"
    RECEPTION_MESSAGE,// "Je reçois un message"

    //Appel Audio
    Debuter_AUDIO_CALL,
    Arreter_AUDIO_CALL,
    Data_AUDIO,

    //Appel Video
    Debuter_Video_CALL,
    Arreter_Video_CALL,
    Data_Appel_Video

}
