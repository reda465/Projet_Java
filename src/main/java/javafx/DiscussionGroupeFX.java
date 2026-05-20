package javafx;

import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import model.Contact;
import model.Groupe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DiscussionGroupeFX {
    private DiscussionGroupeFX() {}
    public static void configurerMenu(
            MenuButton groupMenu,
            javafx.stage.Window owner,
            Supplier<Groupe> groupeActif,
            Supplier<List<Contact>> contactsPourAjout,
            GroupeDiscussionActions actions,
            Runnable afficherAccueil) {
            groupMenu.getItems().clear();
        MenuItem add = new MenuItem("Ajouter membre");
        MenuItem remove = new MenuItem("Retirer membre");
        MenuItem rename = new MenuItem("Renommer");
        MenuItem leave = new MenuItem("Quitter");
        MenuItem delete = new MenuItem("Supprimer");
        groupMenu.getItems().addAll(add, remove, rename, leave, delete);
        add.setOnAction(e -> {
            Groupe g = groupeActif.get();
            if (g == null) return;
            List<Contact> base = contactsPourAjout.get();
            List<Contact> candidats = filtrerContactsPasDansGroupe(base, g);
            if (candidats.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Aucun contact disponible à ajouter (tous sont déjà membres ou liste vide).").showAndWait();
                return;
            }
            SelectionnerContactDialog.choisir(owner, "Ajouter un membre", candidats)
                    .ifPresent(c -> {
                        String num = c.getNumeroTelephone() != null ? c.getNumeroTelephone().trim() : "";
                        if (!num.isEmpty()) actions.ajouterMembreAuGroupe(g.getIdGroupe(), num);
                    });
        });
        remove.setOnAction(e -> {
            Groupe g = groupeActif.get();
            if (g == null) return;
            List<Contact> membres = contactsDepuisNumeros(g.getNumerosMembres(), contactsPourAjout.get());
            if (membres.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Aucun membre listé pour ce groupe. Rouvrez la liste des groupes.").showAndWait();
                return;
            }
            SelectionnerContactDialog.choisir(owner, "Retirer un membre", membres)
                    .ifPresent(c -> {
                        String num = c.getNumeroTelephone() != null ? c.getNumeroTelephone().trim() : "";
                        if (!num.isEmpty()) actions.retirerMembreDuGroupe(g.getIdGroupe(), num);
                    });
        });
        rename.setOnAction(e -> {
            Groupe g = groupeActif.get();
            if (g == null) return;
            TextInputDialog d = new TextInputDialog();
            d.initOwner(owner);
            d.setTitle("Renommer groupe");
            d.setHeaderText(null);
            d.setContentText("Nouveau nom :");
            d.showAndWait().ifPresent(s -> {
                if (!s.isBlank()) actions.modifierNomGroupe(g.getIdGroupe(), s.trim());
            });
        });

        leave.setOnAction(e -> {
            Groupe g = groupeActif.get();
            if (g == null) return;
            actions.quitterGroupe(g.getIdGroupe());
            afficherAccueil.run();
        });

        delete.setOnAction(e -> {
            Groupe g = groupeActif.get();
            if (g == null) return;
            actions.supprimerGroupe(g.getIdGroupe());
            afficherAccueil.run();
        });
    }

    private static List<Contact> filtrerContactsPasDansGroupe(List<Contact> contacts, Groupe g) {
        List<String> dans = g.getNumerosMembres() != null ? g.getNumerosMembres() : List.of();
        List<Contact> out = new ArrayList<>();
        if (contacts == null) return out;
        for (Contact c : contacts) {
            if (c == null || c.getNumeroTelephone() == null) continue;
            String n = c.getNumeroTelephone().trim();
            if (n.isEmpty()) continue;
            boolean deja = false;
            for (String m : dans) {
                if (m != null && m.trim().equals(n)) {
                    deja = true;
                    break;
                }
            }
            if (!deja) out.add(c);
        }
        return out;
    }
    private static List<Contact> contactsDepuisNumeros(List<String> numeros, List<Contact> repertoire) {
        List<Contact> out = new ArrayList<>();
        if (numeros == null) return out;
        for (String num : numeros) {
            if (num == null || num.isBlank()) continue;
            String n = num.trim();
            Contact c = new Contact();
            c.setNumeroTelephone(n);
            c.setNomComplet(n);
            if (repertoire != null) {
                for (Contact r : repertoire) {
                    if (r != null && r.getNumeroTelephone() != null && r.getNumeroTelephone().trim().equals(n)) {
                        if (r.getNomComplet() != null && !r.getNomComplet().isBlank()) {
                            c.setNomComplet(r.getNomComplet().trim());
                        }
                        break;
                    }
                }
            }
            out.add(c);
        }
        return out;
    }
}
