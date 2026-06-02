package fr.retrosphere.gamevault.controller;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.service.GameService;
import fr.retrosphere.gamevault.service.GameValidationException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

public class GameFormController {
    @FXML private Label formTitleLabel;
    @FXML private TextField titleField;
    @FXML private TextField developerField;
    @FXML private TextField publisherField;
    @FXML private TextField yearField;
    @FXML private ComboBox<String> platformCombo;
    @FXML private TextField genreField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Slider ratingSlider;
    @FXML private Label ratingLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label errorLabel;
    @FXML private Button coverButton;

    private final GameService service = new GameService();
    private Consumer<Game> onSaved;
    private Runnable onCancel;
    private Game editedGame;
    private String coverPath = "";

    @FXML
    private void initialize() {
        platformCombo.getItems().setAll("PC", "PS5", "Xbox", "Switch", "Retro", "Dreamcast", "Sega Saturn", "Super Nintendo");
        statusCombo.getItems().setAll("Owned", "Finished", "In Progress", "Wishlist", "Lent");
        ratingSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                ratingLabel.setText(String.format("%.1f", newValue.doubleValue())));
        ratingLabel.setText(String.format("%.1f", ratingSlider.getValue()));
    }

    public void setCallbacks(Consumer<Game> onSaved, Runnable onCancel) {
        this.onSaved = onSaved;
        this.onCancel = onCancel;
    }

    public void edit(Game game) {
        this.editedGame = game;
        formTitleLabel.setText("Edit Vault Entry");
        titleField.setText(game.getTitle());
        developerField.setText(game.getDeveloper());
        publisherField.setText(game.getPublisher());
        yearField.setText(String.valueOf(game.getReleaseYear()));
        platformCombo.setValue(game.getPlatform());
        genreField.setText(game.getGenre());
        statusCombo.setValue(game.getStatus());
        ratingSlider.setValue(game.getRating());
        descriptionArea.setText(game.getDescription());
        coverPath = game.getCoverPath();
        if (coverPath != null && !coverPath.isBlank()) {
            coverButton.setText("▧\n" + new File(coverPath).getName() + "\nClick to replace");
        }
    }

    @FXML
    private void chooseCover() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une jaquette");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"));
        File file = chooser.showOpenDialog(coverButton.getScene().getWindow());
        if (file != null) {
            coverPath = file.getAbsolutePath();
            coverButton.setText("▧\n" + file.getName() + "\nClick to replace");
        }
    }

    @FXML
    private void save() {
        try {
            Game game = editedGame == null ? new Game() : editedGame;
            game.setTitle(titleField.getText());
            game.setDeveloper(developerField.getText());
            game.setPublisher(publisherField.getText());
            game.setReleaseYear(parseYear());
            game.setPlatform(platformCombo.getValue());
            game.setGenre(blankToDefault(genreField.getText(), "Uncategorized"));
            game.setStatus(blankToDefault(statusCombo.getValue(), "Owned"));
            game.setRating(Math.round(ratingSlider.getValue() * 10.0) / 10.0);
            game.setDescription(blankToDefault(descriptionArea.getText(), "Aucune description renseignée."));
            game.setCoverPath(coverPath == null ? "" : coverPath);
            Game saved = service.save(game);
            if (onSaved != null) {
                onSaved.accept(saved);
            }
        } catch (GameValidationException | NumberFormatException exception) {
            errorLabel.setText(exception instanceof NumberFormatException
                    ? "L'année de sortie doit être un nombre valide."
                    : exception.getMessage());
        } catch (RuntimeException exception) {
            errorLabel.setText("Une erreur est survenue pendant l'enregistrement.");
        }
    }

    @FXML
    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private int parseYear() {
        return Integer.parseInt(yearField.getText().trim());
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
