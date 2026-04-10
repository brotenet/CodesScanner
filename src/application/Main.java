package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/FrmMain.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 660, 680);
        scene.getStylesheets().add(getClass().getResource("/resources/css/modena-dark.css").toExternalForm());
        primaryStage.setTitle("QR/Barcode Scanner & Generator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}