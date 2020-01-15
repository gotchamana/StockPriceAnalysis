package stockanalysis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.validation.DoubleValidator;
import com.jfoenix.validation.IntegerValidator;
import com.jfoenix.validation.RequiredFieldValidator;


class StockAnalysisPane extends BorderPane {

	// Package private for convenient access
	JFXButton selectFileBtn;
	JFXButton saveBtn;
	JFXButton analyzeBtn;

	JFXTextField peakDurationInput;
	JFXTextField peakDifferenceInput;
	JFXTextField crashRateInput;
	JFXTextField filePathInput;

	JFXTreeTableView<Tuple> table;
	JFXProgressBar progressBar;
	Label totalLbl;

	StockAnalysisPane() {
		initControls();

		GridPane gridPane = createGridPane();
		gridPane.addRow(0, peakDurationInput, crashRateInput, peakDifferenceInput);

		HBox hBox = new HBox();
		hBox.setHgrow(filePathInput, Priority.SOMETIMES);
		hBox.getStyleClass().add("button-container");
		hBox.getChildren().addAll(filePathInput, selectFileBtn, saveBtn, analyzeBtn);

		VBox vBox = new VBox();
		vBox.setMargin(totalLbl, new Insets(5, 5, 0, 0));
		vBox.getStyleClass().add("vbox");
		vBox.getChildren().addAll(totalLbl, hBox, progressBar);

        getStyleClass().add("stock-price-analysis-pane");
		setTop(gridPane);
		setCenter(table);
		setBottom(vBox);
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
		saveBtn.setDisable(true);

		analyzeBtn = new JFXButton("Analyze");
		analyzeBtn.setDefaultButton(true);

		IntegerValidator integerValidator = new IntegerValidator();
		DoubleValidator doubleValidator = new DoubleValidator();
		RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator("Input required");
		FileExistValidator fileExistValidator = new FileExistValidator();

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
		filePathInput.getValidators().addAll(requiredFieldValidator, fileExistValidator);

		table = createTreeTable();

		progressBar = new JFXProgressBar(0);
		progressBar.prefWidthProperty().bind(widthProperty());

		totalLbl = new Label("Total: 0");
	}

	@SuppressWarnings("unchecked")
	private JFXTreeTableView<Tuple> createTreeTable() {
		Callback<TreeTableColumn<Tuple, LocalDate>, TreeTableCell<Tuple, LocalDate>> dateFormatterCellFactory = col -> new TreeTableCell<>() {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY/MM/dd");

			@Override
			protected void updateItem(LocalDate value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else {
					setText(value.format(formatter));
				}
			}
		};
		Callback<TreeTableColumn<Tuple, Double>,TreeTableCell<Tuple,Double>> doubleFormatterCellFactory = col -> new TreeTableCell<>() {
			@Override
			protected void updateItem(Double value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else {
					setText(String.format("%.2f", value));
				}
			}
		};

		TreeTableColumn<Tuple, LocalDate> crashDateCol = new TreeTableColumn<>("Crash Identification Date");
		TreeTableColumn<Tuple, LocalDate> peakDateCol = new TreeTableColumn<>("Peak Date");
		TreeTableColumn<Tuple, Double> peakStockPriceCol = new TreeTableColumn<>("Index at Peak");
		TreeTableColumn<Tuple, LocalDate> troughDateCol = new TreeTableColumn<>("Trough Date");
		TreeTableColumn<Tuple, Double> troughStockPriceCol = new TreeTableColumn<>("Index at Trough");
		TreeTableColumn<Tuple, Double> peakTroughDeclineCol = new TreeTableColumn<>("Peak-to-Trough decline(%)");
		TreeTableColumn<Tuple, Integer> peakTroughDurationCol = new TreeTableColumn<>("Peak-to-Trough duration(in days)");

		crashDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("crashDate"));
		crashDateCol.setCellFactory(dateFormatterCellFactory);

		peakDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakDate"));
		peakDateCol.setCellFactory(dateFormatterCellFactory);

		peakStockPriceCol.setCellFactory(doubleFormatterCellFactory);
		peakStockPriceCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakStockPrice"));

		troughDateCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("troughDate"));
		troughDateCol.setCellFactory(dateFormatterCellFactory);

		troughStockPriceCol.setCellFactory(doubleFormatterCellFactory);
		troughStockPriceCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("troughStockPrice"));

		peakTroughDeclineCol.setCellFactory(doubleFormatterCellFactory);
		peakTroughDeclineCol.setCellValueFactory(feature -> new ReadOnlyObjectWrapper<>(feature.getValue().getValue().getPeakTroughDecline(3) * 100));

		peakTroughDurationCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakTroughDuration"));

		JFXTreeTableView<Tuple> table = new JFXTreeTableView<>(new TreeItem<Tuple>(null));
		table.setShowRoot(false);
		table.setFocusTraversable(false);
		table.setColumnResizePolicy(JFXTreeTableView.CONSTRAINED_RESIZE_POLICY);
		table.getColumns().addAll(crashDateCol, peakDateCol,
			peakStockPriceCol, troughDateCol, 
			troughStockPriceCol, peakTroughDeclineCol, 
			peakTroughDurationCol);

		return table;
	}
}
