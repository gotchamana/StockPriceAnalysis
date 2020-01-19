package stockanalysis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.validation.DoubleValidator;
import com.jfoenix.validation.IntegerValidator;
import com.jfoenix.validation.RequiredFieldValidator;

class StockAnalysisPane extends JFXTabPane {

	private FontIcon refreshIcon, tableIcon, chartIcon;

	// Package private for convenient access
	JFXButton selectFileBtn;
	JFXButton saveBtn;
	JFXButton analyzeBtn;
	JFXButton refreshBtn;

	JFXTextField peakDurationInput;
	JFXTextField peakDifferenceInput;
	JFXTextField crashRateInput;
	JFXTextField filePathInput;

	JFXTreeTableView<Tuple> table;
	JFXProgressBar progressBar;
	JFXComboBox<Integer> comboBox;
	LineChart<String, Number> chart;
	Pagination pagination;
	Label totalLbl;

	StockAnalysisPane() {
		initControls();

		Tab tableTab = createTableTab();
		tableTab.setClosable(false);

		Tab chartTab = createChartTab();
		chartTab.setClosable(false);

		setSide(Side.LEFT);
		setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		getTabs().addAll(tableTab, chartTab);
	}

	private void initControls() {
		// Icon
		refreshIcon = FontIcon.of(MaterialDesign.MDI_REFRESH);
		refreshIcon.getStyleClass().add("refresh-button-icon");

		tableIcon = FontIcon.of(MaterialDesign.MDI_TABLE_LARGE);
		tableIcon.getStyleClass().add("tab-icon");

		chartIcon = FontIcon.of(MaterialDesign.MDI_CHART_LINE);
		chartIcon.getStyleClass().add("tab-icon");

		// Button
		selectFileBtn = new JFXButton("Select file");

		saveBtn = new JFXButton("Save");
		saveBtn.setDisable(true);

		analyzeBtn = new JFXButton("Analyze");
		analyzeBtn.setDefaultButton(true);

		refreshBtn = new JFXButton(null, refreshIcon);
		refreshBtn.setOpacity(0.3);
		refreshBtn.getStyleClass().add("refresh-chart-button");

		// TextField
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

		// Other
		chart = createLineChart();
		table = createTreeTable();

		comboBox = new JFXComboBox<>();
		comboBox.setPromptText("Number of data drawn");
		comboBox.getItems().addAll(100, 500, 1000, 1500);

		pagination = new Pagination(1, 0);
		pagination.setPageFactory(i -> chart);

		progressBar = new JFXProgressBar(0);
		progressBar.prefWidthProperty().bind(widthProperty());

		totalLbl = new Label("Total: 0");
	}

	private Tab createTableTab() {
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

		BorderPane borderPane = new BorderPane();
		borderPane.getStyleClass().add("stock-price-border-pane");
		borderPane.setTop(gridPane);
		borderPane.setCenter(table);
		borderPane.setBottom(vBox);

		Tab tab = new Tab(null, borderPane);
		tab.setGraphic(tableIcon);

		return tab;
	}

	private Tab createChartTab() {
		AnchorPane anchorPane = new AnchorPane(comboBox);
		AnchorPane.setLeftAnchor(comboBox, 10.0);
		AnchorPane.setBottomAnchor(comboBox, 20.0);
		anchorPane.setPickOnBounds(false);

		StackPane stackPane = new StackPane();
		stackPane.setMargin(refreshBtn, new Insets(10, 10, 0, 0));
		stackPane.getChildren().addAll(pagination, refreshBtn, anchorPane);
		stackPane.getStyleClass().add("stack-pane");

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(stackPane);
		borderPane.getStyleClass().add("stock-price-border-pane");

		Tab tab = new Tab(null, borderPane);
		tab.setGraphic(chartIcon);

		return tab;
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

	private LineChart<String, Number> createLineChart() {
		CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();

		chart = new LineChart<>(xAxis, yAxis);
		chart.setAnimated(false);
		chart.getData().add(new XYChart.Series<>());

		return chart;
	}

	@SuppressWarnings("unchecked")
	private JFXTreeTableView<Tuple> createTreeTable() {
		Callback<TreeTableColumn<Tuple, LocalDate>, TreeTableCell<Tuple, LocalDate>> dateFormatterCellFactory = col -> new TreeTableCell<>() {
			@Override
			protected void updateItem(LocalDate value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else {
					setText(value.format(Util.DATE_FORMATTER));
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
		table.setColumnResizePolicy(JFXTreeTableView.CONSTRAINED_RESIZE_POLICY);
		table.getColumns().addAll(crashDateCol, peakDateCol,
			peakStockPriceCol, troughDateCol, 
			troughStockPriceCol, peakTroughDeclineCol, 
			peakTroughDurationCol);

		return table;
	}
}
