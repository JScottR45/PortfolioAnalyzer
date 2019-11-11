package start;

import com.mashape.unirest.http.Unirest;
import controllers.PortfolioOverviewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The class which starts the entire application. Also responsible for cleanup when application terminates.
 */
public class Main extends Application {
    private PortfolioOverviewController overviewController;

    /**
     * Starts the application.
     *
     * @param primaryStage The top-level container that hosts the main scene.
     * @throws Exception Any exception which may occur if something goes wrong.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../fxml/portfolio_overview.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1460, 950);

        setStyleSheets(scene);

        overviewController = loader.getController();

        primaryStage.setTitle("Portfolio Analyzer");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Writes all new or updated data to disk before terminating the application.
     *
     * @throws Exception Any exception which may occur if something goes wrong.
     */
    @Override
    public void stop() throws Exception {
        super.stop();

        overviewController.shutdown();
        Unirest.shutdown();
    }

    /**
     * Entry point for the application.
     *
     * @param args Any arguments needed to start the application (not used).
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Sets stylesheets for the main screen.
     *
     * @param scene The scene representing the main screen.
     */
    private void setStyleSheets(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("../css/allocation_chart.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/performance_graph.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/table.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/text_input.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/date_picker.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/button.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("../css/checkbox.css").toExternalForm());
    }
}
