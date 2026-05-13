package javafx;

/**
 * Actions réseau / client pour la gestion d'un groupe dans l'interface discussion.
 * Permet d'injecter les appels (p.ex. vers {@link client.ClientHandlerAuth}) sans coupler la vue au singleton.
 */
public interface GroupeDiscussionActions {

    void ajouterMembreAuGroupe(int idGroupe, String numeroTelephone);

    void retirerMembreDuGroupe(int idGroupe, String numeroTelephone);

    void modifierNomGroupe(int idGroupe, String nouveauNom);

    void quitterGroupe(int idGroupe);

    void supprimerGroupe(int idGroupe);
}
