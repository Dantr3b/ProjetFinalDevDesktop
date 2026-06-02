package fr.retrosphere.gamevault.controller;

import fr.retrosphere.gamevault.MainApp;
import fr.retrosphere.gamevault.config.AppConfig;
import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.service.GameService;
import fr.retrosphere.gamevault.service.GameValidationException;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
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
import javafx.stage.FileChooser;

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
    @FXML private HBox topbar;
    @FXML private HBox statusbar;
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
    @FXML private Button favoritesButton;

    private final GameService service = new GameService();
    private String selectedPlatform = "All Platforms";
    private boolean highDensityGrid;
    private boolean french;
    private boolean privateAccount;
    private boolean dataAccessWarningShown;
    private String profileName = "Alex Mercer";
    private String profileBio = "Digital Historian & Collector since 2018";
    private String profileTier = "Pro Curator";
    private String profilePhotoPath = "";

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
        setCollectionChrome(true);
        activate(collectionButton);
        if (favoritesButton != null) favoritesButton.setText("♡");
        List<Game> allGames = safeAllGames();
        List<Game> games = safeSearch(searchField.getText(), selectedPlatform, sortCombo.getValue());
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
                ? "Affichage de " + games.size() + " jeu(x) sur " + allGames.size() + " dans le coffre."
                : "Showing " + games.size() + " of " + allGames.size() + " titles in your archival vault.");
        footer.getStyleClass().add("muted-center");
        page.getChildren().add(footer);
        setContent(page);
        statusLabel.setText(games.size() + (french ? " jeu(x) affiche(s)" : " game(s) shown"));
    }

    @FXML
    private void showFavorites() {
        setCollectionChrome(false);
        activate(null);
        if (favoritesButton != null) favoritesButton.setText("♥");
        List<Game> games = safeFavorites();
        VBox page = pageShell();
        page.getChildren().add(titleBlock("Favoris", games.size() + " / 5 jeux dans vos favoris."));
        if (games.isEmpty()) {
            Label empty = styledLabel("Aucun favori. Cliquez sur ♡ sur une carte de jeu pour en ajouter.", "muted-center");
            page.getChildren().add(empty);
        } else {
            GridPane grid = new GridPane();
            grid.setHgap(22);
            grid.setVgap(22);
            for (int i = 0; i < games.size(); i++) {
                grid.add(gameCard(games.get(i)), i % 4, i / 4);
            }
            page.getChildren().add(grid);
        }
        setContent(page);
        statusLabel.setText("Favoris : " + games.size() + " / 5");
    }

    @FXML
    private void showAddForm() {
        setCollectionChrome(false);
        openForm(null);
    }

    @FXML
    private void showStatistics() {
        setCollectionChrome(false);
        activate(statsButton);
        List<Game> games = safeAllGames();
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
        setCollectionChrome(false);
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
        setCollectionChrome(false);
        activate(profileButton);
        List<Game> games = safeAllGames();
        VBox page = pageShell();
        HBox header = new HBox(32);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("profile-hero");
        StackPane avatar = avatar(initialsFor(profileName), 44);
        VBox identity = new VBox(4, styledLabel(profileName, "page-title"), styledLabel(profileBio, "muted"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, identity, spacer);
        page.getChildren().add(header);
        page.getChildren().add(new HBox(20,
                metric("Games Archived", String.valueOf(games.size()), "collection"),
                metric("Playtime Hours", "8420", "demo profile"),
                metric("Achievements", "432", "tracked")));
        page.getChildren().add(profileActions(games));
        setContent(page);
        statusLabel.setText(french ? "Profil utilisateur" : "User profile");
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
        Button favBtn = new Button(game.isFavorite() ? "♥" : "♡");
        favBtn.getStyleClass().add(game.isFavorite() ? "fav-heart-active" : "fav-heart");
        StackPane.setAlignment(favBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(favBtn, new Insets(6));
        favBtn.setOnAction(event -> {
            try {
                service.toggleFavorite(game);
            } catch (GameValidationException ex) {
                showError(ex.getMessage());
            }
            showCollection();
        });
        cover.getChildren().addAll(platform, rating, favBtn);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("game-meta");
        meta.getChildren().addAll(styledLabel(game.getTitle(), "game-title"),
                styledLabel(game.getGenre().toUpperCase() + "  -  " + game.getStatus().toUpperCase(), "game-subtitle"));
        card.getChildren().addAll(cover, meta);
        return card;
    }

    private void showDetails(Game game) {
        setCollectionChrome(false);
        activate(collectionButton);
        VBox page = pageShell();
        page.setSpacing(36);
        page.getChildren().add(detailHero(game));
        page.getChildren().add(detailBody(game));
        page.getChildren().add(relatedGames(game));
        setContent(page);
        statusLabel.setText("Detail : " + game.getTitle());
    }

    private void showDetailsLegacy(Game game) {
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
        Button favDetail = new Button(game.isFavorite() ? "♥ Retirer des favoris" : "♡ Ajouter aux favoris");
        favDetail.getStyleClass().add(game.isFavorite() ? "fav-heart-active" : "outline-button");
        favDetail.setOnAction(event -> {
            try {
                service.toggleFavorite(game);
            } catch (GameValidationException ex) {
                showError(ex.getMessage());
                return;
            }
            showDetails(game);
        });
        controls.getChildren().addAll(edit, delete, favDetail);
        info.getChildren().add(controls);
        hero.getChildren().addAll(cover, info);
        page.getChildren().add(hero);
        setContent(page);
        statusLabel.setText("Detail : " + game.getTitle());
    }

    private Node detailHero(Game game) {
        HBox hero = new HBox();
        hero.getStyleClass().add("detail-hero-figma");

        StackPane visual = new StackPane();
        visual.getStyleClass().add("detail-visual-pane");
        StackPane cover = cover(game, 420, 560);
        cover.getStyleClass().add("detail-main-cover");

        HBox floating = new HBox(22,
                detailBadge("Current Status", game.getStatus()),
                detailDivider(),
                detailBadge("Platform", game.getPlatform()));
        floating.getStyleClass().add("floating-detail-badge");
        StackPane.setAlignment(cover, Pos.CENTER);
        StackPane.setAlignment(floating, Pos.BOTTOM_LEFT);
        StackPane.setMargin(floating, new Insets(0, 0, 28, 28));
        visual.getChildren().addAll(cover, floating);

        VBox info = new VBox(22);
        info.getStyleClass().add("detail-info-pane");
        HBox tags = new HBox(10, detailTag(game.getGenre(), "cyan-tag"), detailTag(game.getStatus(), "gold-tag"));
        info.getChildren().addAll(
                tags,
                styledLabel(game.getTitle().toUpperCase(), "detail-title-figma"),
                detailsGrid(game),
                ratingBlock(game));

        HBox.setHgrow(visual, Priority.ALWAYS);
        HBox.setHgrow(info, Priority.ALWAYS);
        hero.getChildren().addAll(visual, info);
        return hero;
    }

    private Node detailBody(Game game) {
        HBox body = new HBox(24);
        body.setAlignment(Pos.TOP_LEFT);
        VBox overview = new VBox(22);
        overview.getStyleClass().add("detail-bento-main");
        overview.setPrefWidth(620);
        overview.setMaxWidth(700);
        Label overviewText = styledLabel(game.getDescription(), "description");
        overviewText.getStyleClass().add("detail-overview-text");
        overview.getChildren().addAll(
                styledLabel("Archival Overview", "section-title"),
                overviewText,
                detailMediaStrip(game));

        VBox side = new VBox(22, vaultControls(game), vaultData(game));
        side.setMinWidth(320);
        side.setPrefWidth(320);
        side.setMaxWidth(340);
        HBox.setHgrow(overview, Priority.ALWAYS);
        HBox.setHgrow(side, Priority.NEVER);
        body.getChildren().addAll(overview, side);
        return body;
    }

    private HBox detailMediaStrip(Game game) {
        HBox strip = new HBox(16);
        for (int i = 0; i < 3; i++) {
            StackPane tile = cover(game, 150, 92);
            tile.getStyleClass().add("detail-media-tile");
            strip.getChildren().add(tile);
        }
        return strip;
    }

    private VBox vaultControls(Game game) {
        Button favorite = new Button(game.isFavorite() ? "Remove Favorite" : "Add Favorite");
        favorite.getStyleClass().add(game.isFavorite() ? "primary-button" : "vault-control-primary");
        favorite.setMaxWidth(Double.MAX_VALUE);
        favorite.setOnAction(event -> {
            try {
                service.toggleFavorite(game);
            } catch (GameValidationException ex) {
                showError(ex.getMessage());
                return;
            }
            showDetails(game);
        });

        Button edit = new Button("Edit Entry");
        edit.setOnAction(event -> openForm(game));
        edit.getStyleClass().add("outline-button");
        edit.setMaxWidth(Double.MAX_VALUE);

        Button delete = new Button("Delete from Vault");
        delete.setOnAction(event -> confirmDelete(game));
        delete.getStyleClass().add("danger-button");
        delete.setMaxWidth(Double.MAX_VALUE);

        VBox controls = new VBox(18, styledLabel("Vault Controls", "field-label"), favorite, edit, delete);
        controls.getStyleClass().add("detail-side-card");
        return controls;
    }

    private VBox vaultData(Game game) {
        VBox data = new VBox(12,
                styledLabel("Vault Data", "field-label"),
                vaultDataRow("Added On", game.getAddedAt() == null ? "-" : game.getAddedAt().toLocalDate().toString()),
                vaultDataRow("Storage Used", storageEstimate(game)),
                vaultDataRow("Achievements", achievementEstimate(game)));
        data.getStyleClass().add("detail-side-card");
        return data;
    }

    private Node relatedGames(Game game) {
        List<Game> related = safeAllGames().stream()
                .filter(candidate -> candidate.getId() == null || !candidate.getId().equals(game.getId()))
                .filter(candidate -> game.getPlatform().equals(candidate.getPlatform()))
                .limit(4)
                .toList();
        if (related.isEmpty()) {
            return new VBox();
        }

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(styledLabel("More on " + game.getPlatform(), "section-title"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewFull = new Button("View Full Archival ->");
        viewFull.getStyleClass().add("link-button");
        viewFull.setOnAction(event -> {
            selectedPlatform = game.getPlatform();
            showCollection();
        });
        header.getChildren().addAll(spacer, viewFull);

        HBox cards = new HBox(22);
        related.forEach(candidate -> cards.getChildren().add(gameCard(candidate)));
        return new VBox(22, header, cards);
    }

    private Label detailTag(String value, String styleClass) {
        Label tag = styledLabel(value, styleClass);
        tag.getStyleClass().add("detail-tag");
        return tag;
    }

    private VBox detailBadge(String label, String value) {
        return new VBox(2, styledLabel(label.toUpperCase(), "floating-badge-label"), styledLabel(value, "floating-badge-value"));
    }

    private Region detailDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("detail-badge-divider");
        divider.setPrefWidth(2);
        divider.setPrefHeight(34);
        return divider;
    }

    private HBox vaultDataRow(String label, String value) {
        HBox row = new HBox(12);
        row.getStyleClass().add("vault-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(styledLabel(label, "description"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(spacer, styledLabel(value, "detail-value"));
        return row;
    }

    private String storageEstimate(Game game) {
        int gb = Math.max(8, (int) Math.round(game.getRating() * 9 + game.getReleaseYear() % 7));
        return gb + "." + (game.getReleaseYear() % 10) + " GB";
    }

    private String achievementEstimate(Game game) {
        int total = 50;
        int unlocked = Math.min(total, Math.max(8, (int) Math.round(game.getRating() * 4)));
        return unlocked + " / " + total;
    }

    private GridPane detailsGrid(Game game) {
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(10);
        addDetail(grid, 0, 0, "Developer", game.getDeveloper());
        addDetail(grid, 1, 0, "Publisher", game.getPublisher());
        addDetail(grid, 0, 1, "Platform", game.getPlatform());
        addDetail(grid, 1, 1, "Release Year", String.valueOf(game.getReleaseYear()));
        return grid;
    }

    private void addDetail(GridPane grid, int col, int row, String label, String value) {
        VBox box = new VBox(2, styledLabel(label, "field-label"), styledLabel(value, "detail-value"));
        grid.add(box, col, row);
    }

    private HBox ratingBlock(Game game) {
        StackPane score = new StackPane(styledLabel(String.format("%.1f", game.getRating()), "rating-large"));
        score.getStyleClass().add("rating-ring");
        VBox verdict = new VBox(2,
                styledLabel(game.getRating() >= 9 ? "Masterpiece" : "Curator Approved", "rating-verdict"),
                styledLabel("Based on Vault Ratings", "muted"));
        HBox box = new HBox(24, score, verdict);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("rating-block");
        return box;
    }

    private void confirmDelete(Game game) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer definitivement \"" + game.getTitle() + "\" de la collection ?",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Confirmation de suppression");
        styleDialog(alert, "danger-modal");
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            try {
                service.delete(game);
                showCollection();
                statusLabel.setText("Jeu supprime : " + game.getTitle());
            } catch (RuntimeException exception) {
                showError("Impossible de supprimer ce jeu pour le moment.");
            }
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

    private void setCollectionChrome(boolean visible) {
        topbar.setVisible(visible);
        topbar.setManaged(visible);
        statusbar.setVisible(visible);
        statusbar.setManaged(visible);
    }

    private List<Game> safeAllGames() {
        try {
            dataAccessWarningShown = false;
            return service.allGames();
        } catch (RuntimeException exception) {
            notifyDataAccessProblem();
            return List.of();
        }
    }

    private List<Game> safeSearch(String query, String platform, String sort) {
        try {
            dataAccessWarningShown = false;
            return service.search(query, platform, sort);
        } catch (RuntimeException exception) {
            notifyDataAccessProblem();
            return List.of();
        }
    }

    private List<Game> safeFavorites() {
        try {
            dataAccessWarningShown = false;
            return service.favorites();
        } catch (RuntimeException exception) {
            notifyDataAccessProblem();
            return List.of();
        }
    }

    private void notifyDataAccessProblem() {
        if (!dataAccessWarningShown) {
            dataAccessWarningShown = true;
            statusLabel.setText("Erreur de lecture de la collection.");
            showError("Impossible de lire la collection. Verifiez la base de donnees ou la configuration.");
        }
    }

    private void applyLanguage() {
        collectionButton.setText(french ? "▣  Ma collection" : "▣  My Collection");
        statsButton.setText(french ? "⌁  Statistiques" : "⌁  Statistics");
        settingsButton.setText(french ? "⚙  Parametres" : "⚙  Settings");
        refreshProfileButton();
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

    private Button profileActionButton(String label, String suffix) {
        Button button = new Button(label + "    " + suffix);
        button.getStyleClass().add("profile-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void editProfile() {
        TextField nameField = new TextField(profileName);
        TextField bioField = new TextField(profileBio);
        TextField tierField = new TextField(profileTier);
        TextField photoField = new TextField(profilePhotoPath);
        photoField.setEditable(false);
        photoField.setPromptText(t("No profile photo selected", "Aucune photo de profil selectionnee"));

        StackPane photoPreview = avatar(initialsFor(profileName), 34);
        final String[] selectedPhotoPath = {profilePhotoPath};
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedPhotoMissing(selectedPhotoPath[0])) {
                photoPreview.getChildren().setAll(profilePhotoNode(34, initialsFor(newValue), selectedPhotoPath[0]));
            }
        });
        Button choosePhotoButton = settingsButton(t("Choose Photo", "Choisir une photo"));
        choosePhotoButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(t("Choose profile photo", "Choisir une photo de profil"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"));
            File file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                selectedPhotoPath[0] = copyProfilePhoto(file);
                photoField.setText(selectedPhotoPath[0]);
                photoPreview.getChildren().setAll(profilePhotoNode(34, initialsFor(nameField.getText()), selectedPhotoPath[0]));
            }
        });
        HBox photoRow = new HBox(12, photoPreview, new VBox(8, photoField, choosePhotoButton));
        photoRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(12,
                styledLabel(t("Display name", "Nom affiche"), "field-label"),
                nameField,
                styledLabel(t("Bio", "Bio"), "field-label"),
                bioField,
                styledLabel(t("Membership label", "Statut du membre"), "field-label"),
                tierField,
                styledLabel(t("Profile photo", "Photo de profil"), "field-label"),
                photoRow);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(t("Edit Profile", "Modifier le profil"));
        dialog.setHeaderText(t("Update curator profile", "Mettre a jour le profil"));
        dialog.getDialogPane().setContent(form);
        styleDialog(dialog, "profile-modal");
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                showError(t("The profile name is required.", "Le nom du profil est obligatoire."));
                return;
            }
            profileName = nameField.getText().trim();
            profileBio = blankToDefault(bioField.getText(), profileBio);
            profileTier = blankToDefault(tierField.getText(), profileTier);
            profilePhotoPath = blankToDefault(selectedPhotoPath[0], "");
            applyLanguage();
            showProfile();
            statusLabel.setText(t("Profile updated", "Profil mis a jour"));
        });
    }

    private void exportCollectionCsv() {
        try {
            Files.createDirectories(Path.of("data", "exports"));
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            Path target = Path.of("data", "exports", "gamevault-collection-" + timestamp + ".csv");
            StringBuilder csv = new StringBuilder("Title,Developer,Publisher,Release Year,Platform,Genre,Status,Rating,Favorite\n");
            for (Game game : safeAllGames()) {
                csv.append(csv(game.getTitle())).append(',')
                        .append(csv(game.getDeveloper())).append(',')
                        .append(csv(game.getPublisher())).append(',')
                        .append(game.getReleaseYear()).append(',')
                        .append(csv(game.getPlatform())).append(',')
                        .append(csv(game.getGenre())).append(',')
                        .append(csv(game.getStatus())).append(',')
                        .append(game.getRating()).append(',')
                        .append(game.isFavorite())
                        .append('\n');
            }
            Files.writeString(target, csv.toString());
            showInfo(t("Export created", "Export cree"), target.toAbsolutePath().normalize().toString());
            statusLabel.setText(t("Collection exported", "Collection exportee"));
        } catch (IOException exception) {
            showError(t("Unable to export the collection.", "Impossible d'exporter la collection."));
        }
    }

    private void toggleAccountPrivacy() {
        privateAccount = !privateAccount;
        showInfo(t("Account Privacy", "Confidentialite du compte"),
                privateAccount
                        ? t("Your profile is now private in GameVault.", "Le profil est maintenant prive dans GameVault.")
                        : t("Your profile is now public in GameVault.", "Le profil est maintenant public dans GameVault."));
        showProfile();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) {
            return "AM";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String translateTier(String tier) {
        if ("Pro Curator".equalsIgnoreCase(tier)) {
            return "Conservateur Pro";
        }
        return tier;
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
        Button editProfile = profileActionButton(t("Edit Profile", "Modifier le profil"), ">");
        editProfile.setOnAction(event -> editProfile());

        Button exportCollection = profileActionButton(t("Export Collection", "Exporter la collection"), "CSV");
        exportCollection.setOnAction(event -> exportCollectionCsv());

        Button accountPrivacy = profileActionButton(t("Account Privacy", "Confidentialite du compte"),
                privateAccount ? t("Private", "Prive") : t("Public", "Public"));
        accountPrivacy.setOnAction(event -> toggleAccountPrivacy());

        VBox actions = new VBox(16, styledLabel(t("Quick Actions", "Actions rapides"), "section-title"),
                editProfile,
                exportCollection,
                accountPrivacy);
        actions.getStyleClass().add("panel");
        VBox recent = new VBox(16, styledLabel(t("Recently Archived", "Archives recentes"), "section-title"));
        recent.getStyleClass().add("wide-panel");
        HBox recentCards = new HBox(18);
        games.stream().limit(3).forEach(game -> recentCards.getChildren().add(gameCard(game)));
        recent.getChildren().add(recentCards);
        area.getChildren().addAll(actions, recent);
        return area;
    }

    private String copyProfilePhoto(File file) {
        try {
            Files.createDirectories(Path.of("data", "profile"));
            String extension = extensionOf(file.getName());
            Path target = Path.of("data", "profile", "profile-photo" + extension);
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().normalize().toString();
        } catch (IOException exception) {
            showError(t("Unable to import the profile photo.", "Impossible d'importer la photo de profil."));
            return profilePhotoPath;
        }
    }

    private String extensionOf(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index).toLowerCase() : ".png";
    }

    private boolean selectedPhotoMissing(String photoPath) {
        return photoPath == null || photoPath.isBlank() || !new File(photoPath).exists();
    }

    private void refreshProfileButton() {
        profileButton.setText(profileName + "\n" + (french ? translateTier(profileTier) : profileTier));
        profileButton.setGraphic(profilePhotoNode(24, initialsFor(profileName), profilePhotoPath));
        profileButton.setContentDisplay(ContentDisplay.LEFT);
        profileButton.setGraphicTextGap(12);
    }

    private StackPane avatar(String text, double radius) {
        return new StackPane(profilePhotoNode(radius, text, profilePhotoPath));
    }

    private Node profilePhotoNode(double radius, String fallbackText, String photoPath) {
        if (photoPath != null && !photoPath.isBlank() && new File(photoPath).exists()) {
            ImageView image = new ImageView(new Image(new File(photoPath).toURI().toString(), radius * 2, radius * 2, false, true));
            image.setFitWidth(radius * 2);
            image.setFitHeight(radius * 2);
            image.setPreserveRatio(false);
            image.setClip(new Circle(radius, radius, radius));
            StackPane frame = new StackPane(image);
            frame.getStyleClass().add("profile-photo-frame");
            return frame;
        }
        Circle circle = new Circle(radius);
        circle.getStyleClass().add("avatar-circle");
        Label initials = styledLabel(fallbackText, "avatar-text");
        if (radius < 32) {
            initials.getStyleClass().add("avatar-text-small");
        }
        return new StackPane(circle, initials);
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
        styleDialog(alert, "danger-modal");
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        styleDialog(alert, "info-modal");
        alert.showAndWait();
    }

    private void styleDialog(Alert alert, String extraStyleClass) {
        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(MainApp.class.getResource("/styles/gamevault.css").toExternalForm());
        pane.getStyleClass().add("gamevault-dialog");
        pane.getStyleClass().add(extraStyleClass);
        pane.setMinWidth(460);
        pane.lookupAll(".button").forEach(node -> node.getStyleClass().add("dialog-action-button"));
    }
}
