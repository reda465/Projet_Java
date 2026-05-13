package javafx;

import client.ClientHandlerAuth;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.Groupe;

import java.util.List;
import java.util.function.Consumer;

public class GroupesView extends VBox {
    private final ListView<Groupe> liste = new ListView<>();
    private final Consumer<Groupe> onSelection;

    public GroupesView(Consumer<Groupe> onSelection) {
        this.onSelection = onSelection;
        construireUI();
    }

    private void construireUI() {
        setSpacing(0);
        setStyle("-fx-background-color: #ffffff;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: #25D366;");

        Label titre = new Label("Groupes");
        titre.setFont(Font.font("Segoe UI", 15));
        titre.setTextFill(Color.WHITE);
        Button btnCreer = new Button("+");
        btnCreer.setStyle("-fx-background-color:#128C7E;-fx-text-fill:white;-fx-background-radius:50%;-fx-min-width:34px;-fx-min-height:34px;");
        btnCreer.setOnAction(e -> {
            CreerGroupeDialog dialog = new CreerGroupeDialog();
            dialog.showAndWait().ifPresent(req -> ClientHandlerAuth.getInstance().creerGroupe(req.nomGroupe, req.numeros));
        });

        HBox espace = new HBox();
        HBox.setHgrow(espace, Priority.ALWAYS);
        header.getChildren().addAll(titre, espace, btnCreer);

        liste.setCellFactory(v -> new CelluleGroupe());
        liste.setOnMouseClicked(e -> {
            Groupe g = liste.getSelectionModel().getSelectedItem();
            if (g != null && onSelection != null) onSelection.accept(g);
        });

        getChildren().addAll(header, liste);
        VBox.setVgrow(liste, Priority.ALWAYS);
    }

    public void setGroupes(List<Groupe> groupes) {
        liste.setItems(FXCollections.observableArrayList(groupes));
    }

    private static class CelluleGroupe extends ListCell<Groupe> {
        @Override
        protected void updateItem(Groupe item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            Label nom = new Label(item.getNomGroupe());
            nom.setFont(Font.font("Segoe UI", 14));
            nom.setTextFill(Color.web("#111B21"));
            int nb = item.getNumerosMembres() != null ? item.getNumerosMembres().size() : 0;
            Label sousTitre = new Label(nb + " membres");
            sousTitre.setFont(Font.font("Segoe UI", 12));
            sousTitre.setTextFill(Color.web("#667781"));
            VBox box = new VBox(2, nom, sousTitre);
            box.setPadding(new Insets(8, 10, 8, 10));
            setGraphic(box);
        }
    }
}
