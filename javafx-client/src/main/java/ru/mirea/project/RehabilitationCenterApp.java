package ru.mirea.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.project.controller.MainController;

public class RehabilitationCenterApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(RehabilitationCenterApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Инициализация интерфейса JavaFX...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_view.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root, 1150, 750);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setTitle("Информационная система реабилитационного центра (JavaFX + PostgreSQL)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(620);
            primaryStage.show();

            logger.info("Главное окно успешно открыто");
        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске JavaFX-приложения: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

