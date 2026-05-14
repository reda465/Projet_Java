package javafx;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.Contact;

import java.util.ArrayList;
import java.util.List;

public class CreerGroupeDialog extends Dialog<CreerGroupeDialog.ResultatCreation> {
    private final TextField nomField = new TextField();
    private final List<CheckBox> membres = new ArrayList<>();
    private List<Contact> contacts = null;

    public CreerGroupeDialog(List<Contact> contacts) {
        this.contacts = contacts != null ? contacts : new ArrayList<>();
        setTitle("Créer un groupe");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().add(new Label("Nom du groupe"));
        root.getChildren().add(nomField);

        root.getChildren().add(new Label("Choisir au moins 2 membres"));
        for (Contact contact : this.contacts) {
            String numero = contact.getNumeroTelephone() != null ? contact.getNumeroTelephone().trim() : "";
            if (numero.isEmpty()) continue;
            String nom = contact.getNomComplet() != null && !contact.getNomComplet().isBlank()
                    ? contact.getNomComplet().trim() : numero;
            CheckBox c = new CheckBox(nom + " (" + numero + ")");
            c.setUserData(numero);
            membres.add(c);
            root.getChildren().add(c);
        }
        getDialogPane().setContent(root);

        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String nom = nomField.getText() != null ? nomField.getText().trim() : "";
            int nbSelection = 0;
            for (CheckBox c : membres) if (c.isSelected()) nbSelection++;
            if (nom.isEmpty()) {
                ev.consume();
                return;
            }
            if (nbSelection < 2) {
                ev.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                List<String> selection = new ArrayList<>();
                for (CheckBox c : membres) if (c.isSelected()) selection.add(String.valueOf(c.getUserData()));
                return new ResultatCreation(nomField.getText() != null ? nomField.getText().trim() : "", selection.toArray(new String[0]));
            }
            return null;
        });
    }

    public CreerGroupeDialog() {

    }
    public static class ResultatCreation {
        public final String nomGroupe;
        public final String[] numeros;

        public ResultatCreation(String nomGroupe, String[] numeros) {
            this.nomGroupe = nomGroupe;
            this.numeros = numeros;
        }
    }
}
