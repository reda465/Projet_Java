package network;

// enum = ÉNUMÉRATION = liste de valeurs fixes
// C'est comme dire : les seules couleurs possibles sont ROUGE, VERT, BLEU
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
    RECEPTION_MESSAGE // "Je reçois un message"
}
