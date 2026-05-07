package javafx;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CreerGroupeDialog extends Dialog<CreerGroupeDialog.ResultatCreation> {
    private final TextField nomField = new TextField();
    private final List<CheckBox> membres = new ArrayList<>();

    public CreerGroupeDialog() {
        setTitle("Créer un groupe");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().add(new Label("Nom du groupe"));
        root.getChildren().add(nomField);

        // Simplicité V1: saisie manuelle de numéros prédéfinis
        String[] nums = {"0600000001", "0600000002", "0600000003", "0600000004"};
        for (String n : nums) {
            CheckBox c = new CheckBox(n);
            membres.add(c);
            root.getChildren().add(c);
        }
        getDialogPane().setContent(root);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                List<String> selection = new ArrayList<>();
                for (CheckBox c : membres) if (c.isSelected()) selection.add(c.getText());
                return new ResultatCreation(nomField.getText() != null ? nomField.getText().trim() : "", selection.toArray(new String[0]));
            }
            return null;
        });
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
