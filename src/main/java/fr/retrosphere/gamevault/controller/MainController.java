package fr.retrosphere.gamevault.controller;

import fr.retrosphere.gamevault.MainApp;
import fr.retrosphere.gamevault.config.AppConfig;
import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.service.GameService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    private static final List<String> PLATFORMS = List.of("All Platforms", "PC", "PS5", "Xbox", "Switch", "Retro");

    @FXML private BorderPane root;
    @FXML private StackPane contentPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label statusLabel;
    @FXML private Button collectionButton;
    @FXML private Button statsButton;
    @FXML private Button settingsButton;
    @FXML private Button profileButton;
    @FXML private Button addGameButton;
    @FXML private Label sortByLabel;

    private final GameService service = new GameService();
    private String selectedPlatform = "All Platforms";
    private boolean highDensityGrid;
    private boolean french;

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("Newest Added", "Title", "Rating", "Release Year"));
        sortCombo.setValue("Newest Added");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> showCollection());
        sortCombo.valueProperty().addListener((observable, oldValue, newValue) -> showCollection());
        applyLanguage();
        showCollection();
    }

    @FXML
    public void showCollection() {
        activate(collectionButton);
        List<Game> games = service.search(searchField.getText(), selectedPlatform, sortCombo.getValue());
        VBox page = pageShell();
        page.getChildren().add(filterBar());

        GridPane grid = new GridPane();
        grid.setHgap(highDensityGrid ? 14 : 22);
        grid.setVgap(highDensityGrid ? 14 : 22);
        int columns = highDensityGrid ? 5 : 4;
        for (int i = 0; i < games.size(); i++) {
            grid.add(gameCard(games.get(i)), i % columns, i / columns);
        }

        page.getChildren().add(grid);
        Label footer = new Label(french
                ? "Affichage de " + games.size() + " jeu(x) sur " + service.allGames().size() + " dans le coffre."
                : "Showing " + games.size() + " of " + service.allGames().size() + " titles in your archival vault.");
        footer.getStyleClass().add("muted-center");
        page.getChildren().add(footer);
        setContent(page);
        statusLabel.setText(games.size() + (french ? " jeu(x) affiche(s)" : " game(s) shown"));
    }

    @FXML
    private void showAddForm() {
        openForm(null);
    }

    @FXML
    private void showStatistics() {
        activate(statsButton);
        List<Game> games = service.allGames();
        VBox page = pageShell();
        page.getChildren().add(titleBlock(t("Collection Statistics", "Statistiques de collection"),
                t("An overview of your digital archive performance and library growth.", "Vue d'ensemble de votre archive et de sa croissance.")));

        HBox cards = new HBox(18,
                metric(t("Total Games", "Total de jeux"), String.valueOf(games.size()), "+ ready"),
                metric(t("Platforms Tracked", "Plateformes suivies"), String.valueOf(distinctPlatforms(games)), t("systems", "systemes")),
                metric(t("Average Rating", "Note moyenne"), String.format("%.2f", averageRating(games)), t("vault score", "score du coffre")),
                metric(t("Last Added", "Dernier ajout"), games.isEmpty() ? "-" : games.get(0).getTitle(), t("newest", "recent")));
        cards.getStyleClass().add("metrics-row");
        page.getChildren().add(cards);

        HBox charts = new HBox(20, growthChart(games), platformDistribution(games));
        page.getChildren().add(charts);
        page.getChildren().add(topRatedTable(games));
        setContent(page);
        statusLabel.setText(t("Statistics computed", "Statistiques calculees"));
    }

    @FXML
    private void showSettings() {
        activate(settingsButton);
        VBox page = pageShell();
        page.getChildren().add(titleBlock(t("Settings", "Parametres"),
                t("Manage your vault environment and archival preferences.", "Gerez l'environnement du coffre et vos preferences d'archivage.")));

        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().setAll("English (US)", "Francais");
        languageCombo.setValue(french ? "Francais" : "English (US)");
        languageCombo.getStyleClass().add("setting-control");
        languageCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill: #dae2fd;");
            }
        });
        languageCombo.setOnAction(event -> {
            french = "Francais".equals(languageCombo.getValue());
            applyLanguage();
            showSettings();
        });

        ToggleButton darkModeToggle = new ToggleButton(root.getStyleClass().contains("light-mode") ? "Disabled" : "Enabled");
        darkModeToggle.setSelected(!root.getStyleClass().contains("light-mode"));
        darkModeToggle.getStyleClass().add("settings-toggle");
        darkModeToggle.setOnAction(event -> {
            boolean enabled = darkModeToggle.isSelected();
            darkModeToggle.setText(enabled ? "Enabled" : "Disabled");
            root.getStyleClass().remove("light-mode");
            if (!enabled) {
                root.getStyleClass().add("light-mode");
            }
            statusLabel.setText(enabled ? "Mode sombre active" : "Mode clair active");
        });

        ToggleButton densityToggle = new ToggleButton(highDensityGrid ? "Enabled" : "Disabled");
        densityToggle.setSelected(highDensityGrid);
        densityToggle.getStyleClass().add("settings-toggle");
        densityToggle.setOnAction(event -> {
            highDensityGrid = densityToggle.isSelected();
            densityToggle.setText(highDensityGrid ? "Enabled" : "Disabled");
            statusLabel.setText(highDensityGrid ? "Grille dense activee" : "Grille dense desactivee");
        });

        Button browseButton = settingsButton(t("Open Folder", "Ouvrir dossier"));
        browseButton.setOnAction(event -> openDatabaseFolder());

        Button backupButton = settingsButton(t("Backup Library", "Sauvegarder"));
        backupButton.getStyleClass().add("outline-button");
        backupButton.setOnAction(event -> backupDatabase("backup"));

        Button restoreButton = settingsButton(t("Create Restore Point", "Point de restauration"));
        restoreButton.setOnAction(event -> backupDatabase("restore-point"));

        Button figmaButton = settingsButton(t("Show Figma Links", "Afficher Figma"));
        figmaButton.setOnAction(event -> showInfo("Maquette Figma",
                "Les liens Figma sont listes dans le README et dans application.properties."));

        page.getChildren().add(settingsPanel(t("General", "General"),
                List.of(rowWithControl(t("Interface Language", "Langue de l'interface"),
                        t("Select the UI language used by GameVault.", "Selectionne la langue utilisee par GameVault."), languageCombo))));
        page.getChildren().add(settingsPanel(t("Appearance", "Apparence"),
                List.of(rowWithControl(t("Dark Mode", "Mode sombre"),
                                t("Use high-contrast dark foundations for reduced eye strain.", "Utilise un fond sombre contraste pour limiter la fatigue visuelle."), darkModeToggle),
                        rowWithControl(t("High Density Grid", "Grille dense"),
                                t("Show more games on screen by reducing card width.", "Affiche plus de jeux en reduisant la largeur des cartes."), densityToggle))));
        page.getChildren().add(settingsPanel(t("Database & Backup", "Base de donnees et sauvegarde"),
                List.of(rowWithControl(t("Database Path", "Chemin de la base"), AppConfig.get("database.url", "data/gamevault.db"), browseButton),
                        rowWithControl(t("Backup Library", "Sauvegarde de la bibliotheque"),
                                t("Copy the SQLite database into data/backups.", "Copie la base SQLite dans data/backups."), backupButton),
                        rowWithControl(t("Restore Point", "Point de restauration"),
                                t("Create a timestamped local restore point.", "Cree une copie locale horodatee."), restoreButton),
                        rowWithControl(t("Figma Mockup", "Maquette Figma"),
                                t("Reference used for this interface.", "Reference utilisee pour cette interface."), figmaButton))));

        setContent(page);
        statusLabel.setText(t("Settings", "Parametres"));
    }

    @FXML
    private void showProfile() {
        activate(profileButton);
        List<Game> games = service.allGames();
        VBox page = pageShell();
        HBox header = new HBox(32);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("profile-hero");
        StackPane avatar = avatar("AM");
        VBox identity = new VBox(4, styledLabel("Alex Mercer", "page-title"), styledLabel("Digital Historian & Collector since 2018", "muted"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button share = new Button("Share");
        share.getStyleClass().add("ghost-button");
        Button follow = new Button("Follow");
        follow.getStyleClass().add("primary-button");
        header.getChildren().addAll(avatar, identity, spacer, share, follow);
        page.getChildren().add(header);
        page.getChildren().add(new HBox(20,
                metric("Games Archived", String.valueOf(games.size()), "collection"),
                metric("Playtime Hours", "8420", "demo profile"),
                metric("Achievements", "432", "tracked")));
        page.getChildren().add(profileActions(games));
        setContent(page);
        statusLabel.setText("Profil utilisateur");
    }

    private void openForm(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/game-form.fxml"));
            Node node = loader.load();
            GameFormController controller = loader.getController();
            controller.setCallbacks(saved -> {
                showCollection();
                statusLabel.setText("Jeu enregistre : " + saved.getTitle());
            }, this::showCollection);
            if (game != null) {
                controller.edit(game);
            }
            setContent(node);
            statusLabel.setText(game == null ? "Ajout d'un jeu" : "Modification : " + game.getTitle());
        } catch (IOException exception) {
            showError("Impossible d'ouvrir le formulaire.");
        }
    }

    private HBox filterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        for (String platform : PLATFORMS) {
            Button button = new Button(platform);
            button.getStyleClass().add(platform.equals(selectedPlatform) ? "filter-chip-active" : "filter-chip");
            button.setOnAction(event -> {
                selectedPlatform = platform;
                showCollection();
            });
            bar.getChildren().add(button);
        }
        return bar;
    }

    private Node gameCard(Game game) {
        double cardWidth = highDensityGrid ? 170 : 210;
        double coverHeight = highDensityGrid ? 170 : 210;

        VBox card = new VBox();
        card.getStyleClass().add("game-card");
        card.setPrefWidth(cardWidth);
        card.setOnMouseClicked(event -> showDetails(game));

        StackPane cover = cover(game, cardWidth, coverHeight);
        Label platform = new Label(game.getPlatform().toUpperCase());
        platform.getStyleClass().add("platform-badge");
        StackPane.setAlignment(platform, Pos.TOP_LEFT);
        StackPane.setMargin(platform, new Insets(10));
        Label rating = new Label("STAR " + String.format("%.1f", game.getRating()));
        rating.getStyleClass().add("rating-corner");
        StackPane.setAlignment(rating, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(rating, new Insets(10));
        cover.getChildren().addAll(platform, rating);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("game-meta");
        meta.getChildren().addAll(styledLabel(game.getTitle(), "game-title"),
                styledLabel(game.getGenre().toUpperCase() + "  -  " + game.getStatus().toUpperCase(), "game-subtitle"));
        card.getChildren().addAll(cover, meta);
        return card;
    }

    private void showDetails(Game game) {
        activate(collectionButton);
        VBox page = pageShell();
        HBox hero = new HBox(36);
        hero.getStyleClass().add("detail-hero");
        StackPane cover = cover(game, 340, 420);
        VBox info = new VBox(18);
        info.getChildren().addAll(
                styledLabel(game.getGenre() + " - " + game.getPlatform(), "cyan-label"),
                styledLabel(game.getTitle().toUpperCase(), "detail-title"),
                detailsGrid(game),
                ratingBlock(game),
                styledLabel("Archival Overview", "section-title"),
                styledLabel(game.getDescription(), "description"));
        HBox controls = new HBox(14);
        Button edit = new Button("Edit Entry");
        edit.setOnAction(event -> openForm(game));
        edit.getStyleClass().add("outline-button");
        Button delete = new Button("Delete from Vault");
        delete.setOnAction(event -> confirmDelete(game));
        delete.getStyleClass().add("danger-button");
        controls.getChildren().addAll(edit, delete);
        info.getChildren().add(controls);
        hero.getChildren().addAll(cover, info);
        page.getChildren().add(hero);
        setContent(page);
        statusLabel.setText("Detail : " + game.getTitle());
    }

    private GridPane detailsGrid(Game game) {
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(10);
        addDetail(grid, 0, 0, "Developer", game.getDeveloper());
        addDetail(grid, 1, 0, "Publisher", game.getPublisher());
        addDetail(grid, 0, 1, "Release Year", String.valueOf(game.getReleaseYear()));
        addDetail(grid, 1, 1, "Status", game.getStatus());
        return grid;
    }

    private void addDetail(GridPane grid, int col, int row, String label, String value) {
        VBox box = new VBox(2, styledLabel(label, "field-label"), styledLabel(value, "detail-value"));
        grid.add(box, col, row);
    }

    private VBox ratingBlock(Game game) {
        VBox box = new VBox(2);
        box.getStyleClass().add("rating-block");
        box.getChildren().addAll(styledLabel(String.format("%.1f", game.getRating()), "rating-large"),
                styledLabel(game.getRating() >= 9 ? "Masterpiece" : "Curator Approved", "muted"));
        return box;
    }

    private void confirmDelete(Game game) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer definitivement \"" + game.getTitle() + "\" de la collection ?",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Confirmation de suppression");
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            service.delete(game);
            showCollection();
            statusLabel.setText("Jeu supprime : " + game.getTitle());
        });
    }

    private StackPane cover(Game game, double width, double height) {
        StackPane cover = new StackPane();
        cover.setPrefSize(width, height);
        cover.getStyleClass().add("cover");
        if (game.getCoverPath() != null && !game.getCoverPath().isBlank() && new File(game.getCoverPath()).exists()) {
            ImageView image = new ImageView(new Image(new File(game.getCoverPath()).toURI().toString(), width, height, false, true));
            image.setFitWidth(width);
            image.setFitHeight(height);
            image.setPreserveRatio(false);
            cover.getChildren().add(image);
        } else {
            Label initials = new Label(game.initials());
            initials.getStyleClass().add("cover-initials");
            cover.getChildren().add(initials);
        }
        return cover;
    }

    private VBox pageShell() {
        VBox page = new VBox(30);
        page.setPadding(new Insets(40));
        page.getStyleClass().add("page");
        return page;
    }

    private void setContent(Node node) {
        contentPane.getChildren().setAll(node);
    }

    private void applyLanguage() {
        collectionButton.setText(french ? "▣  Ma collection" : "▣  My Collection");
        statsButton.setText(french ? "⌁  Statistiques" : "⌁  Statistics");
        settingsButton.setText(french ? "⚙  Parametres" : "⚙  Settings");
        profileButton.setText(french ? "AM  Alex Mercer\n     Conservateur Pro" : "AM  Alex Mercer\n     Pro Curator");
        addGameButton.setText(french ? "+ AJOUTER UN JEU" : "+ ADD GAME");
        searchField.setPromptText(french ? "Rechercher dans la collection..." : "Search your archive...");
        sortByLabel.setText(french ? "Trier par :" : "Sort by:");

        String selectedSort = sortCombo.getValue();
        sortCombo.getItems().setAll(
                t("Newest Added", "Ajout recent"),
                t("Title", "Titre"),
                t("Rating", "Note"),
                t("Release Year", "Annee de sortie"));
        if (selectedSort == null) {
            sortCombo.setValue(sortCombo.getItems().get(0));
        } else if (selectedSort.equals("Title") || selectedSort.equals("Titre")) {
            sortCombo.setValue(t("Title", "Titre"));
        } else if (selectedSort.equals("Rating") || selectedSort.equals("Note")) {
            sortCombo.setValue(t("Rating", "Note"));
        } else if (selectedSort.equals("Release Year") || selectedSort.equals("Annee de sortie")) {
            sortCombo.setValue(t("Release Year", "Annee de sortie"));
        } else {
            sortCombo.setValue(t("Newest Added", "Ajout recent"));
        }
    }

    private String t(String english, String frenchText) {
        return french ? frenchText : english;
    }

    private VBox titleBlock(String title, String subtitle) {
        return new VBox(4, styledLabel(title, "page-title-small"), styledLabel(subtitle, "muted"));
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private VBox metric(String title, String value, String caption) {
        VBox box = new VBox(8, styledLabel(caption, "metric-caption"), styledLabel(title, "muted"), styledLabel(value, "metric-value"));
        box.getStyleClass().add("metric-card");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Node growthChart(List<Game> games) {
        Map<Integer, Long> byYear = games.stream().collect(Collectors.groupingBy(Game::getReleaseYear, Collectors.counting()));
        VBox panel = new VBox(20, styledLabel("Library Growth", "section-title"));
        panel.getStyleClass().add("wide-panel");
        HBox bars = new HBox(10);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        byYear.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            VBox barWrap = new VBox(8);
            barWrap.setAlignment(Pos.BOTTOM_CENTER);
            Region bar = new Region();
            bar.setPrefSize(54, Math.max(36, entry.getValue() * 42));
            bar.getStyleClass().add("chart-bar");
            barWrap.getChildren().addAll(bar, styledLabel(String.valueOf(entry.getKey()), "muted"));
            bars.getChildren().add(barWrap);
        });
        panel.getChildren().add(bars);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private Node platformDistribution(List<Game> games) {
        VBox panel = new VBox(14, styledLabel("Platform Distribution", "section-title"));
        panel.getStyleClass().add("side-panel");
        games.stream().collect(Collectors.groupingBy(Game::getPlatform, Collectors.counting()))
                .forEach((platform, count) -> panel.getChildren().add(styledLabel(platform + "   " + count + " title(s)", "muted")));
        return panel;
    }

    private Node topRatedTable(List<Game> games) {
        VBox panel = new VBox(12, styledLabel("Top Rated Games", "section-title"));
        panel.getStyleClass().add("panel");
        games.stream().sorted(Comparator.comparingDouble(Game::getRating).reversed()).limit(5)
                .forEach(game -> panel.getChildren().add(new HBox(20,
                        styledLabel(game.getTitle(), "detail-value"),
                        styledLabel(game.getPlatform(), "platform-text"),
                        styledLabel(String.valueOf(game.getReleaseYear()), "muted"),
                        styledLabel(String.format("%.1f STAR", game.getRating()), "gold-label"))));
        return panel;
    }

    private int distinctPlatforms(List<Game> games) {
        return (int) games.stream().map(Game::getPlatform).distinct().count();
    }

    private double averageRating(List<Game> games) {
        return games.stream().mapToDouble(Game::getRating).average().orElse(0);
    }

    private Node settingsPanel(String title, List<HBox> rows) {
        VBox panel = new VBox(18, styledLabel(title, "section-title"));
        panel.getStyleClass().add("panel");
        panel.getChildren().addAll(rows);
        return panel;
    }

    private HBox rowWithControl(String title, String subtitle, Node control) {
        VBox copy = new VBox(2, styledLabel(title, "detail-value"), styledLabel(subtitle, "muted"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16, copy, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button settingsButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("setting-action-button");
        return button;
    }

    private void openDatabaseFolder() {
        Path folder = Path.of("data").toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
                statusLabel.setText("Dossier data ouvert");
            } else {
                showInfo("Dossier de base de donnees", folder.toString());
            }
        } catch (IOException exception) {
            showError("Impossible d'ouvrir ou de creer le dossier data.");
        }
    }

    private void backupDatabase(String prefix) {
        Path database = Path.of("data", "gamevault.db");
        if (!Files.exists(database)) {
            showError("La base de donnees n'existe pas encore. Lance l'application une premiere fois avant la sauvegarde.");
            return;
        }
        try {
            Files.createDirectories(Path.of("data", "backups"));
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            Path target = Path.of("data", "backups", "gamevault-" + prefix + "-" + timestamp + ".db");
            Files.copy(database, target, StandardCopyOption.REPLACE_EXISTING);
            showInfo("Sauvegarde creee", target.toAbsolutePath().normalize().toString());
            statusLabel.setText("Sauvegarde creee : " + target.getFileName());
        } catch (IOException exception) {
            showError("Impossible de creer la sauvegarde de la base.");
        }
    }

    private Node profileActions(List<Game> games) {
        HBox area = new HBox(24);
        VBox actions = new VBox(16, styledLabel("Quick Actions", "section-title"),
                styledLabel("Edit Profile    >", "detail-value"),
                styledLabel("Export Collection    CSV / PDF", "detail-value"),
                styledLabel("Account Privacy    >", "detail-value"));
        actions.getStyleClass().add("panel");
        VBox recent = new VBox(16, styledLabel("Recently Archived", "section-title"));
        recent.getStyleClass().add("wide-panel");
        HBox recentCards = new HBox(18);
        games.stream().limit(3).forEach(game -> recentCards.getChildren().add(gameCard(game)));
        recent.getChildren().add(recentCards);
        area.getChildren().addAll(actions, recent);
        return area;
    }

    private StackPane avatar(String text) {
        Circle circle = new Circle(44);
        circle.getStyleClass().add("avatar-circle");
        return new StackPane(circle, styledLabel(text, "avatar-text"));
    }

    private void activate(Button active) {
        List.of(collectionButton, statsButton, settingsButton, profileButton)
                .forEach(button -> button.getStyleClass().remove("nav-button-active"));
        if (active != null && !active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Erreur GameVault");
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
