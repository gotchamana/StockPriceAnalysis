package stockanalysis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import stockanalysis.control.Controller;
import stockanalysis.view.StockAnalysisPane;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        StockAnalysisPane root = new StockAnalysisPane();

        Controller controller = new Controller(primaryStage, root);
        controller.init();

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/style.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("StockAnalysis");
        primaryStage.getIcons().addAll(new Image("icon16.png"), new Image("icon32.png"), new Image("icon64.png"));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
