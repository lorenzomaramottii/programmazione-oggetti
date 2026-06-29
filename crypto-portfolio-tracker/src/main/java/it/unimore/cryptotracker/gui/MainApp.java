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

        Scene scene = new Scene(view, 850, 550);

        // Carica il file CSS dalle risorse dell'applicazione
        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Impossibile caricare il file CSS: " + e.getMessage());
        }

        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }
}