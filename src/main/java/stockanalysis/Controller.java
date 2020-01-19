package stockanalysis;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jooq.lambda.Unchecked;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

public class Controller {
	
    private Analyzer analyzer;
    private ObjectProperty<List<Tuple>> tupleProperty;
	private List<StockPrice> data;

	private Stage stage;
	private StockAnalysisPane root;

	public Controller(Stage stage, StockAnalysisPane root) {
		this.stage = stage;
		this.root = root;

		analyzer = new Analyzer();
		tupleProperty = new SimpleObjectProperty<>();
	}

	public void init() {
		tupleProperty.addListener((obs, oldValue, newValue) -> {
			// Update save button
			root.saveBtn.setDisable(newValue.isEmpty());

			// Update total number label
			root.totalLbl.setText("Total: " + newValue.size());

			// updatePagination(newValue);
			updateTable(newValue);
		});

		root.saveBtn.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save Analysis Result");
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.setInitialFileName("analysis.txt");

			Optional.ofNullable(fileChooser.showSaveDialog(stage))
				.ifPresent(file -> {
					Task<Void> saveTask = new Task<>() {
						@Override
						protected Void call() throws Exception {
							Util.saveAnalysisResult(tupleProperty.get(), file.toPath());
							return null;
						}
					};
					saveTask.setOnRunning(event -> root.progressBar.setProgress(-1));
					saveTask.setOnSucceeded(event -> root.progressBar.setProgress(0));

					Thread thread = new Thread(saveTask);
					thread.setDaemon(true);
					thread.start();
				});
		});

		root.selectFileBtn.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select CSV File");
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv", "*.CSV"));

			Optional.ofNullable(fileChooser.showOpenDialog(stage))
				.ifPresent(file -> root.filePathInput.setText(file.getPath()));
		});

		root.analyzeBtn.setOnAction(e -> {
			JFXTextField peakDurationInput = root.peakDurationInput;
			JFXTextField peakDifferenceInput = root.peakDifferenceInput;
			JFXTextField crashRateInput = root.crashRateInput;
			JFXTextField filePathInput = root.filePathInput;

			if (peakDurationInput.validate() & peakDifferenceInput.validate() &
					crashRateInput.validate() & filePathInput.validate()) {
				int peakDuration = Integer.parseInt(peakDurationInput.getText());
				double peakDifference = Double.parseDouble(peakDifferenceInput.getText()) / 100;
				double crashRate = Double.parseDouble(crashRateInput.getText()) / 100;
				String path = filePathInput.getText();
				Function<String, Path> convertToRealPath = Unchecked.function(p -> Paths.get(p).toRealPath());
				data = Util.parseData(convertToRealPath.apply(path));

				analyzer.setCrashRate(crashRate);
				analyzer.setPeakDuration(peakDuration);
				analyzer.setPeakDifference(peakDifference);
				analyzer.setData(data);

				Task<List<Tuple>> analyzeTask = new Task<>() {
					@Override
					protected List<Tuple> call() throws Exception {
						return analyzer.getAnalysisResult();
					}
				};
				analyzeTask.setOnRunning(event -> root.progressBar.setProgress(-1));
				analyzeTask.setOnSucceeded(event -> {
					root.progressBar.setProgress(0);
					tupleProperty.set(analyzeTask.getValue());
				});

				Thread thread = new Thread(analyzeTask);
				thread.setDaemon(true);
				thread.start();
			}
		});

		setRefreshBtnBehavior();
	}

	private void setRefreshBtnBehavior() {
		JFXButton refreshBtn = root.refreshBtn;

		RotateTransition rt = new RotateTransition(Duration.millis(500), refreshBtn);
		rt.setByAngle(360);
		rt.setOnFinished(e -> Optional.ofNullable(tupleProperty.get()).ifPresent(this::updatePagination));

		FadeTransition ft = new FadeTransition(Duration.millis(100), refreshBtn);

		refreshBtn.setOnAction(e -> {
			rt.play();
		});

		refreshBtn.setOnMouseEntered(e -> {
			if (ft.getStatus() != Animation.Status.RUNNING) {
				ft.setFromValue(0.3);
				ft.setToValue(1);
				ft.play();
			}
		});

		refreshBtn.setOnMouseExited(e -> {
			if (ft.getStatus() != Animation.Status.RUNNING) {
				ft.setFromValue(1);
				ft.setToValue(0.3);
				ft.play();
			}
		});
	}

	private void updatePagination(List<Tuple> newTuples) {
		int drawnDataNumber = Optional.ofNullable(root.comboBox.getSelectionModel().getSelectedItem()).orElse(1000);

		Pagination pagination = root.pagination;

		pagination.setPageFactory(i -> {
			updateChart(drawnDataNumber, i, newTuples);
			return root.chart;
		});

		int pageCount = data.size() / drawnDataNumber;
		pageCount = (data.size() % drawnDataNumber == 0) ? pageCount : pageCount + 1;
		pagination.setPageCount(pageCount);

		pagination.setCurrentPageIndex(0);
		pagination.setMaxPageIndicatorCount(5);
	}

	@SuppressWarnings("unchecked")
	private void updateChart(int drawnDataNumber, int skipFactor, List<Tuple> newTuples) {
		Set[] peakTroughs = newTuples.stream()
			.map(t -> {
				Set<StockPrice> peak = new HashSet<>();
				peak.add(t.getPeak());

				Set<StockPrice> trough = new HashSet<>();
				trough.add(t.getTrough());

				return new Set[] { peak, trough };
			})
			.reduce(new Set[] { new HashSet<StockPrice>(), new HashSet<StockPrice>() }, (r, e) -> {
				r[0].addAll(e[0]);
				r[1].addAll(e[1]);

				return r;
			});

		List<XYChart.Data<String, Number>> chartData = data.stream()
			.skip(drawnDataNumber * skipFactor)
			.limit(drawnDataNumber)
			.map(sp -> {
				String date = sp.getDate().format(Util.DATE_FORMATTER);
				Number price = sp.getPrice();

				return new XYChart.Data<>(date, price, sp);
			})

			// Configure XYChart.Data
			.peek(spData -> {
				StockPrice sp = (StockPrice) spData.getExtraValue();

				if (peakTroughs[0].contains(sp)) {
					Node symbol = createSymbol("Peak", sp);
					spData.setNode(symbol);
				} else if (peakTroughs[1].contains(sp)) {
					Node symbol = createSymbol("Trough", sp);
					spData.setNode(symbol);
				}
			})
			.collect(Collectors.toList());

		root.chart.getData()
			.get(0)
			.getData()
			.setAll(chartData);
	}

	private Node createSymbol(String type, StockPrice sp) {
		Circle symbol = new Circle(5);
		symbol.setOnMouseEntered(e -> symbol.setRadius(6));
		symbol.setOnMouseExited(e -> symbol.setRadius(5));
		symbol.getStyleClass().addAll("chart-symbol", "chart-" + type.toLowerCase());

		Tooltip tooltip = new Tooltip(String.format("%s: %s%n%s: %s%n%s: %.2f",
			"Type", type,
			"Date", sp.getDate().format(Util.DATE_FORMATTER),
			"Price", sp.getPrice()));
		Tooltip.install(symbol, tooltip);

		return symbol;
	}

	private void updateTable(List<Tuple> newTuples) {
		TreeItem<Tuple> rootItem = root.table.getRoot();
		rootItem.getChildren().clear();

		newTuples.stream()
			.map(TreeItem::new)
			.forEach(rootItem.getChildren()::add);
	}
}
