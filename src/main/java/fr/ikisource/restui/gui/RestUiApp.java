package fr.ikisource.restui.gui;

import fr.ikisource.restui.conf.App;
import fr.ikisource.restui.controller.ControllerManager;
import fr.ikisource.restui.controller.MainController;
import fr.ikisource.restui.service.Initializer;
import fr.ikisource.restui.service.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main class of the application
 *
 * @author olivier
 * @since 1.0
 */
public class RestUiApp extends Application {

    @Override
    public void start(final Stage primaryStage) throws Exception {

        MainController mainController = ControllerManager.getMainController();
        primaryStage.setTitle(App.TITLE);
        primaryStage.getIcons().add(new Image(ControllerManager.class.getResource(App.APPLICATION_ICON).toString()));
        primaryStage.setScene(new Scene(mainController.getRootNode()));

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            closeApplication();
        });

        primaryStage.show();

        Logger.info("Application started");
    }

    public static void main(final String[] args) {

        Initializer.build();

        launch(args);
    }

    private void closeApplication() {

        ControllerManager.getMainController().exit(null);
    }

}
