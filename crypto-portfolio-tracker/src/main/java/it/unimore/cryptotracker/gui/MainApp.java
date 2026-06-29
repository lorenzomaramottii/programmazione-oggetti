package it.unimore.cryptotracker.gui;

import it.unimore.cryptotracker.database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.initializeDatabase();

        primaryStage.setTitle("Crypto Portfolio Tracker Pro - Dashboard");

        CryptoController controller = new CryptoController();
        TabPane view = controller.getView();

        Scene scene = new Scene(view, 900, 600);

        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Impossibile trovare il file style.css nelle risorse.");
        }

        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }
}