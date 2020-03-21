package stockanalysis.view;

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
import javafx.scene.control.ToggleGroup;
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
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.validation.DoubleValidator;
import com.jfoenix.validation.IntegerValidator;
import com.jfoenix.validation.RequiredFieldValidator;

import stockanalysis.model.StockPriceCrashCycle;
import stockanalysis.model.Tuple;
import stockanalysis.util.FileExistValidator;
import stockanalysis.util.Util;

public class StockAnalysisPane extends JFXTabPane {

	private FontIcon refreshIcon, tableIcon, chartIcon;

	public JFXRadioButton analysisTableRBtn;
	public JFXRadioButton crashCycleTableWithPeakRBtn;
	public JFXRadioButton crashCycleTableWithCrashRBtn;

	public JFXButton selectFileBtn;
	public JFXButton saveAnalysisBtn;
	public JFXButton analyzeBtn;
	public JFXButton refreshBtn;
	public JFXButton saveChartBtn;

	public JFXTextField crashRateInput;
	public JFXTextField filePathInput;
	public JFXTextField widthInput;
	public JFXTextField heightInput;

	public JFXProgressBar progressBar;
	public JFXProgressBar chartProgressBar;

	public JFXTreeTableView<Tuple> analysisTable;
	public JFXTreeTableView<StockPriceCrashCycle> crashCycleTableWithPeak;
	public JFXTreeTableView<StockPriceCrashCycle> crashCycleTableWithCrash;

	public JFXComboBox<Integer> comboBox;
	public LineChart<String, Number> chart;
	public Pagination pagination;
	public Label totalLbl;

	public StockAnalysisPane() {
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

		// Radio Button
		ToggleGroup radioBtns = new ToggleGroup();

		analysisTableRBtn = new JFXRadioButton("Analysis Table");
		analysisTableRBtn.setSelected(true);
		analysisTableRBtn.setToggleGroup(radioBtns);

		crashCycleTableWithPeakRBtn = new JFXRadioButton("Crash Cycle Table (Peak)");
		crashCycleTableWithPeakRBtn.setToggleGroup(radioBtns);

		crashCycleTableWithCrashRBtn = new JFXRadioButton("Crash Cycle Table (Identification)");
		crashCycleTableWithCrashRBtn.setToggleGroup(radioBtns);

		// Button
		selectFileBtn = new JFXButton("Select file");

		saveAnalysisBtn = new JFXButton("Save");
		saveAnalysisBtn.setDisable(true);

		analyzeBtn = new JFXButton("Analyze");
		analyzeBtn.setDefaultButton(true);

		refreshBtn = new JFXButton(null, refreshIcon);
		refreshBtn.setOpacity(0.3);
		refreshBtn.getStyleClass().add("refresh-chart-button");

		saveChartBtn = new JFXButton("Save");
		saveChartBtn.setDisable(true);

		// TextField
		IntegerValidator integerValidator = new IntegerValidator();
		DoubleValidator doubleValidator = new DoubleValidator();
		RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator("Input required");
		FileExistValidator fileExistValidator = new FileExistValidator();

		crashRateInput = new JFXTextField();
		crashRateInput.setPromptText("Crash identification (%)");
		crashRateInput.getValidators().addAll(requiredFieldValidator, doubleValidator);

		filePathInput = new JFXTextField();
		filePathInput.setPromptText("CSV file path");
		filePathInput.getValidators().addAll(requiredFieldValidator, fileExistValidator);

		widthInput = new JFXTextField();
		widthInput.setPromptText("Width");
		widthInput.getValidators().addAll(requiredFieldValidator, integerValidator);
		widthInput.getStyleClass().add("chart-dimension-text-field");

		heightInput = new JFXTextField();
		heightInput.setPromptText("Height");
		heightInput.getValidators().addAll(requiredFieldValidator, integerValidator);
		heightInput.getStyleClass().add("chart-dimension-text-field");

		// Table
		analysisTable = createAnalysisTable();
		crashCycleTableWithPeak = createCrashCycleTable();
		crashCycleTableWithCrash = createCrashCycleTable();

		// Other
		chart = Util.createLineChart();

		comboBox = new JFXComboBox<>();
		comboBox.setPromptText("Number of data drawn");
		comboBox.getItems().addAll(100, 500, 1000, 1500);
		comboBox.getValidators().add(requiredFieldValidator);

		pagination = new Pagination(1, 0);
		pagination.setPageFactory(i -> chart);

		progressBar = new JFXProgressBar(0);
		progressBar.prefWidthProperty().bind(widthProperty());

		chartProgressBar = new JFXProgressBar(0);

		totalLbl = new Label("Total: 0");
	}

	@SuppressWarnings("unchecked")
	private JFXTreeTableView<Tuple> createAnalysisTable() {
		Callback<TreeTableColumn<Tuple, LocalDate>, TreeTableCell<Tuple, LocalDate>> dateFormatterCellFactory = col -> new TreeTableCell<>() {
			@Override
			protected void updateItem(LocalDate value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else if (value.equals(LocalDate.MIN)) {
					setText("N/A");
				} else {
					setText(value.format(Util.DATE_FORMATTER));
				}
			}
		};
		Callback<TreeTableColumn<Tuple, Double>,TreeTableCell<Tuple, Double>> doubleFormatterCellFactory = col -> new TreeTableCell<>() {
			@Override
			protected void updateItem(Double value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else if (Double.isInfinite(value)) {
					setText("N/A");
				} else {
					setText(String.format("%.2f", value));
				}
			}
		};
		Callback<TreeTableColumn<Tuple, Integer>, TreeTableCell<Tuple, Integer>> durationFormatterCellFactory = col -> new TreeTableCell<>() {
			@Override
			protected void updateItem(Integer value, boolean empty) {
				super.updateItem(value, empty);
				if (empty) {
					setText(null);
				} else if (value < 0) {
					setText("N/A");
				} else {
					setText(String.valueOf(value));
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
		peakTroughDeclineCol.setCellValueFactory(feature -> new ReadOnlyObjectWrapper<>(feature.getValue().getValue().getPeakTroughDecline() * 100));

		peakTroughDurationCol.setCellFactory(durationFormatterCellFactory);
		peakTroughDurationCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("peakTroughDuration"));

		JFXTreeTableView<Tuple> analysisTable = new JFXTreeTableView<>(new TreeItem<Tuple>(null));
		analysisTable.setShowRoot(false);
		analysisTable.setColumnResizePolicy(JFXTreeTableView.CONSTRAINED_RESIZE_POLICY);
		analysisTable.getColumns().addAll(crashDateCol, peakDateCol,
			peakStockPriceCol, troughDateCol, 
			troughStockPriceCol, peakTroughDeclineCol, 
			peakTroughDurationCol);

		return analysisTable;
	}

	@SuppressWarnings("unchecked")
	private JFXTreeTableView<StockPriceCrashCycle> createCrashCycleTable() {
		Callback<TreeTableColumn<StockPriceCrashCycle, LocalDate>, TreeTableCell<StockPriceCrashCycle, LocalDate>> dateFormatterCellFactory = col -> new TreeTableCell<>() {
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
		Callback<TreeTableColumn<StockPriceCrashCycle, Double>,TreeTableCell<StockPriceCrashCycle, Double>> doubleFormatterCellFactory = col -> new TreeTableCell<>() {
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
		
		TreeTableColumn<StockPriceCrashCycle, LocalDate> stockPriceDateCol = new TreeTableColumn<>("Date");
		TreeTableColumn<StockPriceCrashCycle, Double> stockPriceIndexCol = new TreeTableColumn<>("Index");
		TreeTableColumn<StockPriceCrashCycle, Boolean> inCrashCycleCol = new TreeTableColumn<>("In Crash Cycle");

		stockPriceDateCol.setCellFactory(dateFormatterCellFactory);
		stockPriceDateCol.setCellValueFactory(feature -> new ReadOnlyObjectWrapper<>(feature.getValue().getValue().getStockPrice().getDate()));

		stockPriceIndexCol.setCellFactory(doubleFormatterCellFactory);
		stockPriceIndexCol.setCellValueFactory(feature -> new ReadOnlyObjectWrapper<>(feature.getValue().getValue().getStockPrice().getPrice()));

		inCrashCycleCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("inCrashCycle"));

		JFXTreeTableView<StockPriceCrashCycle> crashCycleTable = new JFXTreeTableView<>(new TreeItem<StockPriceCrashCycle>(null));
		crashCycleTable.setShowRoot(false);
		crashCycleTable.setVisible(false);
		crashCycleTable.setColumnResizePolicy(JFXTreeTableView.CONSTRAINED_RESIZE_POLICY);
		crashCycleTable.getColumns().addAll(stockPriceDateCol, stockPriceIndexCol, inCrashCycleCol);

		return crashCycleTable;
	}

	private Tab createTableTab() {
		HBox hBox = new HBox();
		hBox.getStyleClass().add("radio-buttons-hbox");
		hBox.getChildren().addAll(analysisTableRBtn, crashCycleTableWithPeakRBtn, crashCycleTableWithCrashRBtn);

		GridPane gridPane = createGridPane();
		gridPane.addRow(0, crashRateInput, hBox);

		HBox hBox2 = new HBox();
		hBox2.setHgrow(filePathInput, Priority.SOMETIMES);
		hBox2.getStyleClass().addAll("hbox", "table-tab-hbox");
		hBox2.getChildren().addAll(filePathInput, selectFileBtn, saveAnalysisBtn, analyzeBtn);

		VBox vBox = new VBox();
		vBox.setMargin(totalLbl, new Insets(5, 5, 0, 0));
		vBox.getStyleClass().add("vbox");
		vBox.getChildren().addAll(totalLbl, hBox2, progressBar);

		StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll(crashCycleTableWithPeak, crashCycleTableWithCrash, analysisTable);

		BorderPane borderPane = new BorderPane();
		borderPane.getStyleClass().add("stock-price-border-pane");
		borderPane.setTop(gridPane);
		borderPane.setCenter(stackPane);
		borderPane.setBottom(vBox);

		Tab tab = new Tab(null, borderPane);
		tab.setGraphic(tableIcon);

		return tab;
	}

	private GridPane createGridPane() {
		ColumnConstraints col = new ColumnConstraints(10, GridPane.USE_COMPUTED_SIZE,
			GridPane.USE_COMPUTED_SIZE, Priority.SOMETIMES,
			HPos.CENTER, true);

		ColumnConstraints col2 = new ColumnConstraints(10, GridPane.USE_COMPUTED_SIZE,
			GridPane.USE_COMPUTED_SIZE, Priority.NEVER,
			HPos.CENTER, true);

		GridPane gridPane = new GridPane();
		gridPane.getColumnConstraints().addAll(col, col2);
		gridPane.getStyleClass().add("grid-pane");

		return gridPane;
	}

	private Tab createChartTab() {
		HBox hBox = new HBox();
		hBox.getChildren().addAll(widthInput, heightInput, saveChartBtn);
		hBox.getStyleClass().addAll("hbox");

		AnchorPane anchorPane = new AnchorPane(comboBox, hBox, chartProgressBar);
		anchorPane.setPickOnBounds(false);

		AnchorPane.setLeftAnchor(comboBox, 10.0);
		AnchorPane.setBottomAnchor(comboBox, 30.0);

		AnchorPane.setRightAnchor(hBox, 10.0);
		AnchorPane.setBottomAnchor(hBox, 30.0);

		AnchorPane.setLeftAnchor(chartProgressBar, 0.0);
		AnchorPane.setRightAnchor(chartProgressBar, 0.0);
		AnchorPane.setBottomAnchor(chartProgressBar, 0.0);

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
}
