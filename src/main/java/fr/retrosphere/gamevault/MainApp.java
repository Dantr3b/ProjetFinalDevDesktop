package fr.retrosphere.gamevault;

import fr.retrosphere.gamevault.persistence.HibernateUtil;
import fr.retrosphere.gamevault.service.GameSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene;
        try {
            HibernateUtil.initialize();
            new GameSeeder().seedIfEmpty();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-layout.fxml"));
            scene = new Scene(loader.load(), 1280, 820);
        } catch (IOException | RuntimeException exception) {
            scene = new Scene(startupErrorScreen(exception), 1280, 820);
        }
        scene.getStylesheets().add(MainApp.class.getResource("/styles/gamevault.css").toExternalForm());
        stage.setTitle("GameVault - RetroSphere");
        stage.setMinWidth(1060);
        stage.setMinHeight(720);
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    private VBox startupErrorScreen(Exception exception) {
        // A database or configuration failure is displayed inside JavaFX instead of crashing the process.
        Label title = new Label("GameVault ne peut pas demarrer correctement.");
        title.getStyleClass().add("page-title-small");
        Label message = new Label("Verifiez la configuration, les droits d'ecriture du dossier data ou la base SQLite.");
        message.getStyleClass().add("description");
        Label technical = new Label(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        technical.getStyleClass().add("error-label");
        VBox box = new VBox(18, title, message, technical);
        box.getStyleClass().add("startup-error");
        return box;
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
