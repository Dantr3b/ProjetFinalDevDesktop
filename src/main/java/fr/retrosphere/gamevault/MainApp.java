package fr.retrosphere.gamevault;

import fr.retrosphere.gamevault.persistence.HibernateUtil;
import fr.retrosphere.gamevault.service.GameSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        HibernateUtil.initialize();
        new GameSeeder().seedIfEmpty();

        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-layout.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 820);
        scene.getStylesheets().add(MainApp.class.getResource("/styles/gamevault.css").toExternalForm());
        stage.setTitle("GameVault - RetroSphere");
        stage.setMinWidth(1060);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
