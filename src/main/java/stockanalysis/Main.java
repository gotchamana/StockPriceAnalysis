package stockanalysis;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.validation.DoubleValidator;
import com.jfoenix.validation.IntegerValidator;
import com.jfoenix.validation.RequiredFieldValidator;

public class Main extends Application {

    private Analyzer analyzer = new Analyzer();

    @Override
    public void start(Stage primaryStage) {
        StockAnalysisPane root = new StockAnalysisPane();
        root.getStyleClass().add("stock-price-analysis-pane");

        root.selectFileBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv", "*.CSV"));

            Optional.ofNullable(fileChooser.showOpenDialog(primaryStage))
                .ifPresent(file -> root.filePathInput.setText(file.getPath()));
        });

        root.analyzeBtn.setOnAction(e -> {
            JFXTextField peakDurationInput = root.peakDurationInput;
            JFXTextField peakDifferenceInput = root.peakDifferenceInput;
            JFXTextField crashRateInput = root.crashRateInput;
            JFXTextField filePathInput = root.filePathInput;

            if (peakDurationInput.validate() & peakDifferenceInput.validate() &
                    crashRateInput.validate() & filePathInput.validate()) {
                TreeItem<Tuple> rootItem = root.table.getRoot();
                rootItem.getChildren().clear();

                int peakDuration = Integer.parseInt(peakDurationInput.getText());
                double peakDifference = Double.parseDouble(peakDifferenceInput.getText()) / 100;
                double crashRate = Double.parseDouble(crashRateInput.getText()) / 100;
                String path = filePathInput.getText();

                analyzer.setCrashRate(crashRate);
                analyzer.setPeakDuration(peakDuration);
                analyzer.setPeakDifference(peakDifference);
                analyzer.setPath(path);

                new Thread(() -> {
                    analyzer.getAnalysisResult()
                        .stream()
                        .map(TreeItem::new)
                        .forEach(rootItem.getChildren()::add);
                }).start();
            }
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/style.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("StockPriceAnalysis");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class StockAnalysisPane extends BorderPane {

        // Package private for convenient access
        JFXButton selectFileBtn;
        JFXButton saveBtn;
        JFXButton analyzeBtn;

        JFXTextField peakDurationInput;
        JFXTextField peakDifferenceInput;
        JFXTextField crashRateInput;
        JFXTextField filePathInput;

        JFXTreeTableView<Tuple> table;

        private StockAnalysisPane() {
            initControls();

            GridPane gridPane = createGridPane();
            gridPane.addRow(0, peakDurationInput, crashRateInput, peakDifferenceInput);

            HBox hBox = new HBox();
            hBox.setHgrow(filePathInput, Priority.SOMETIMES);
            hBox.getStyleClass().add("button-container");
            hBox.getChildren().addAll(filePathInput, selectFileBtn, saveBtn, analyzeBtn);

            setTop(gridPane);
            setCenter(table);
            setBottom(hBox);

            JFXProgressBar bar = new JFXProgressBar(-1);
            bar.setSecondaryProgress(-1);
        }

        private GridPane createGridPane() {
            ColumnConstraints col = new ColumnConstraints(10, GridPane.USE_COMPUTED_SIZE,
                    GridPane.USE_COMPUTED_SIZE, Priority.SOMETIMES,
                    HPos.CENTER, true);

            GridPane gridPane = new GridPane();
            gridPane.getColumnConstraints().addAll(col, col, col);
            gridPane.getStyleClass().add("grid-pane");

            return gridPane;
        }

        private void initControls() {
            selectFileBtn = new JFXButton("Select file");
            saveBtn = new JFXButton("Save");
            analyzeBtn = new JFXButton("Analyze");

            IntegerValidator integerValidator = new IntegerValidator();
            DoubleValidator doubleValidator = new DoubleValidator();
            RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator("Input required");

            peakDurationInput = new JFXTextField();
            peakDurationInput.setPromptText("Duration between peaks (in days)");
            peakDurationInput.getValidators().addAll(requiredFieldValidator, integerValidator);

            peakDifferenceInput = new JFXTextField();
            peakDifferenceInput.setPromptText("Stock price between peaks (%)");
            peakDifferenceInput.getValidators().addAll(requiredFieldValidator, doubleValidator);

            crashRateInput = new JFXTextField();
            crashRateInput.setPromptText("Crash identification (%)");
            crashRateInput.getValidators().addAll(requiredFieldValidator, doubleValidator);

            filePathInput = new JFXTextField();
            filePathInput.setPromptText("CSV file path");
            // filePathInput.setEditable(false);
            filePathInput.getValidators().addAll(requiredFieldValidator);

            table = createTreeTable();
        }

        @SuppressWarnings("unchecked")
        private JFXTreeTableView<Tuple> createTreeTable() {
            TreeTableColumn<Tuple, LocalDate> crashDateCol = new TreeTableColumn<>("Crash Identification Date");
            TreeTableColumn<Tuple, LocalDate> peakDateCol = new TreeTableColumn<>("Peak Date");
            TreeTableColumn<Tuple, Double> peakStockPriceCol = new TreeTableColumn<>("Index at Peak");
            TreeTableColumn<Tuple, LocalDate> troughDateCol = new TreeTableColumn<>("Trough Date");
            TreeTableColumn<Tuple, Double> troughStockPriceCol = new TreeTableColumn<>("Index at Trough");
            // TreeTableColumn<Tuple, Double> peakTroughDeclineCol = new TreeTableColumn<>("Peak-to-Trough decline(%)");
            TreeTableColumn<Tuple, String> peakTroughDeclineCol = new TreeTableColumn<>("Peak-to-Trough decline(%)");
            TreeTableColumn<Tuple, Integer> peakTroughDurationCol = new TreeTableColumn<>("Peak-to-Trough duration(in days)");

            crashDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("crashDate"));
            peakDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakDate"));
            peakStockPriceCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakStockPrice"));
            troughDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("troughDate"));
            troughStockPriceCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("troughStockPrice"));
            // peakTroughDeclineCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getValue().getPeakTroughDecline(3) * 100));
            peakTroughDeclineCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(String.format("%.2f", c.getValue().getValue().getPeakTroughDecline() * 100)));
            peakTroughDurationCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakTroughDuration"));

            TreeItem<Tuple> root = new TreeItem<>(new Tuple(null, null, null));
            StockPrice sp = new StockPrice(LocalDate.now(), 100);
            root.getChildren().add(new TreeItem<>(new Tuple(sp, sp, sp)));

            JFXTreeTableView<Tuple> table = new JFXTreeTableView<>(root);
            table.setShowRoot(false);
            table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
            table.getColumns().addAll(crashDateCol, peakDateCol,
                    peakStockPriceCol, troughDateCol, 
                    troughStockPriceCol, peakTroughDeclineCol, 
                    peakTroughDurationCol);

            return table;
        }
    }
}
