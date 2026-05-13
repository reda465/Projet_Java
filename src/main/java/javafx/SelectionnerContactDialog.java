package javafx;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.Contact;

import java.util.List;
import java.util.Optional;

/**
 * Boîte de dialogue pour choisir un contact dans une liste (ajout / retrait de membre de groupe).
 */
public final class SelectionnerContactDialog {

    private SelectionnerContactDialog() {}

    public static Optional<Contact> choisir(javafx.stage.Window owner, String titre, List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) return Optional.empty();

        Dialog<Contact> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle(titre);
        d.setHeaderText(null);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ListView<Contact> list = new ListView<>();
        list.getItems().setAll(contacts);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Contact c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    String nom = c.getNomComplet() != null && !c.getNomComplet().isBlank()
                            ? c.getNomComplet().trim() : c.getNumeroTelephone();
                    String num = c.getNumeroTelephone() != null ? c.getNumeroTelephone().trim() : "";
                    setText(nom + (num.isEmpty() || nom.equals(num) ? "" : " (" + num + ")"));
                }
            }
        });
        list.setPrefHeight(Math.min(360, 44 + contacts.size() * 36));

        VBox root = new VBox(10, new Label("Sélectionnez un contact :"), list);
        root.setPadding(new Insets(12));
        d.getDialogPane().setContent(root);

        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (list.getSelectionModel().getSelectedItem() == null) ev.consume();
        });

        d.setResultConverter(btn -> btn == ButtonType.OK ? list.getSelectionModel().getSelectedItem() : null);
        return d.showAndWait();
    }
}
