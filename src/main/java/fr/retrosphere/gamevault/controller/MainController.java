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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    private static final List<String> PLATFORMS = List.of("All Platforms", "PC", "PS5", "Xbox", "Switch", "Retro");

    @FXML private StackPane contentPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label statusLabel;
    @FXML private Button collectionButton;
    @FXML private Button statsButton;
    @FXML private Button settingsButton;
    @FXML private Button profileButton;
    @FXML private Button favoritesButton;

    private final GameService service = new GameService();
    private String selectedPlatform = "All Platforms";

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("Newest Added", "Title", "Rating", "Release Year"));
        sortCombo.setValue("Newest Added");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> showCollection());
        sortCombo.valueProperty().addListener((observable, oldValue, newValue) -> showCollection());
        showCollection();
    }

    @FXML
    public void showCollection() {
        activate(collectionButton);
        if (favoritesButton != null) favoritesButton.setText("♡");
        List<Game> games = service.search(searchField.getText(), selectedPlatform, sortCombo.getValue());
        VBox page = pageShell();
        page.getChildren().add(filterBar());

        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(22);
        for (int i = 0; i < games.size(); i++) {
            Game game = games.get(i);
            grid.add(gameCard(game), i % 4, i / 4);
        }
        page.getChildren().add(grid);
        Label footer = new Label("Showing " + games.size() + " of " + service.allGames().size() + " titles in your archival vault.");
        footer.getStyleClass().add("muted-center");
        page.getChildren().add(footer);
        setContent(page);
        statusLabel.setText(games.size() + " jeu(x) affiché(s)");
    }

    @FXML
    private void showFavorites() {
        activate(null);
        if (favoritesButton != null) favoritesButton.setText("♥");
        List<Game> games = service.favorites();
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
        openForm(null);
    }

    @FXML
    private void showStatistics() {
        activate(statsButton);
        List<Game> games = service.allGames();
        VBox page = pageShell();
        page.getChildren().add(titleBlock("Collection Statistics", "An overview of your digital archive performance and library growth."));

        HBox cards = new HBox(18, metric("Total Games", String.valueOf(games.size()), "+ ready"),
                metric("Platforms Tracked", String.valueOf(distinctPlatforms(games)), "systems"),
                metric("Average Rating", String.format("%.2f", averageRating(games)), "vault score"),
                metric("Last Added", games.isEmpty() ? "-" : games.get(0).getTitle(), "newest"));
        cards.getStyleClass().add("metrics-row");
        page.getChildren().add(cards);

        HBox charts = new HBox(20, growthChart(games), platformDistribution(games));
        page.getChildren().add(charts);
        page.getChildren().add(topRatedTable(games));
        setContent(page);
        statusLabel.setText("Statistiques calculées");
    }

    @FXML
    private void showSettings() {
        activate(settingsButton);
        VBox page = pageShell();
        page.getChildren().add(titleBlock("Settings", "Manage your vault environment and archival preferences."));
        page.getChildren().add(settingsPanel("General",
                List.of(row("Interface Language", "Français / English", "English (US)"),
                        row("Auto-save Frequency", "Determine how often archival edits are saved.", "Every 15 minutes"))));
        page.getChildren().add(settingsPanel("Appearance",
                List.of(row("Dark Mode", "High-contrast dark foundations for reduced eye strain.", "Enabled"),
                        row("High Density Grid", "Show more games on screen by reducing card padding.", "Disabled"))));
        page.getChildren().add(settingsPanel("Database & Backup",
                List.of(row("Database Path", "Configured in application.properties.", AppConfig.get("database.url", "data/gamevault.db")),
                        row("Figma Mockup", "Reference used for this interface.", "Linked in README"))));
        setContent(page);
        statusLabel.setText("Paramètres");
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
                statusLabel.setText("Jeu enregistré : " + saved.getTitle());
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
        VBox card = new VBox();
        card.getStyleClass().add("game-card");
        card.setPrefWidth(210);
        card.setOnMouseClicked(event -> showDetails(game));

        StackPane cover = cover(game, 210, 210);
        Label platform = new Label(game.getPlatform().toUpperCase());
        platform.getStyleClass().add("platform-badge");
        StackPane.setAlignment(platform, Pos.TOP_LEFT);
        StackPane.setMargin(platform, new Insets(10));
        Label rating = new Label("★ " + String.format("%.1f", game.getRating()));
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
                styledLabel(game.getGenre().toUpperCase() + "  •  " + game.getStatus().toUpperCase(), "game-subtitle"));
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
                styledLabel(game.getGenre() + " • " + game.getPlatform(), "cyan-label"),
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
        statusLabel.setText("Détail : " + game.getTitle());
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
                "Supprimer définitivement \"" + game.getTitle() + "\" de la collection ?",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Confirmation de suppression");
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            service.delete(game);
            showCollection();
            statusLabel.setText("Jeu supprimé : " + game.getTitle());
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
                        styledLabel(game.getReleaseYear() + "", "muted"),
                        styledLabel(String.format("%.1f ★", game.getRating()), "gold-label"))));
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

    private HBox row(String title, String subtitle, String value) {
        VBox copy = new VBox(2, styledLabel(title, "detail-value"), styledLabel(subtitle, "muted"));
        Label valueLabel = styledLabel(value, "setting-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16, copy, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node profileActions(List<Game> games) {
        HBox area = new HBox(24);
        VBox actions = new VBox(16, styledLabel("Quick Actions", "section-title"),
                styledLabel("Edit Profile    ›", "detail-value"),
                styledLabel("Export Collection    CSV / PDF", "detail-value"),
                styledLabel("Account Privacy    ›", "detail-value"));
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
        List.of(collectionButton, statsButton, settingsButton, profileButton).forEach(button -> button.getStyleClass().remove("nav-button-active"));
        if (active != null && !active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Erreur GameVault");
        alert.showAndWait();
    }
}
