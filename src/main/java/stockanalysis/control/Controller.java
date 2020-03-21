package stockanalysis.control;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Pagination;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jooq.lambda.Unchecked;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import stockanalysis.model.Analyzer;
import stockanalysis.model.StockPrice;
import stockanalysis.model.StockPriceCrashCycle;
import stockanalysis.model.Tuple;
import stockanalysis.util.ChartSaver;
import stockanalysis.util.Util;
import stockanalysis.view.StockAnalysisPane;

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
			// Update save analysis button
			root.saveAnalysisBtn.setDisable(newValue.isEmpty());

			// Update save chart button
			root.saveChartBtn.setDisable(newValue.isEmpty());

			// Update total number label
			root.totalLbl.setText("Total: " + newValue.size());

			updateAnalysisTable(newValue);
			updateCrashCycleTable(newValue);
		});

		// Configure RadioButton
		root.analysisTableRBtn.setOnAction(e -> {
			root.analysisTable.setVisible(true);
			root.crashCycleTableWithPeak.setVisible(false);
			root.crashCycleTableWithCrash.setVisible(false);
		});

		root.crashCycleTableWithPeakRBtn.setOnAction(e -> {
			root.analysisTable.setVisible(false);
			root.crashCycleTableWithPeak.setVisible(true);
			root.crashCycleTableWithCrash.setVisible(false);
		});

		root.crashCycleTableWithCrashRBtn.setOnAction(e -> {
			root.analysisTable.setVisible(false);
			root.crashCycleTableWithPeak.setVisible(false);
			root.crashCycleTableWithCrash.setVisible(true);
		});

		// Configure Button
		root.saveAnalysisBtn.setOnAction(e -> {
			FileChooser fileChooser = createFileChooser("Save Analysis Result", "analysis.txt");
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
			FileChooser fileChooser = createFileChooser("Select CSV File", "", new FileChooser.ExtensionFilter("CSV File", "*.csv", "*.CSV"));
			Optional.ofNullable(fileChooser.showOpenDialog(stage))
				.ifPresent(file -> root.filePathInput.setText(file.getPath()));
		});

		root.analyzeBtn.setOnAction(e -> {
			JFXTextField crashRateInput = root.crashRateInput;
			JFXTextField filePathInput = root.filePathInput;

			if (crashRateInput.validate() & filePathInput.validate()) {
				double crashRate = Double.parseDouble(crashRateInput.getText()) / 100;
				String path = filePathInput.getText();
				Function<String, Path> convertToRealPath = Unchecked.function(p -> Paths.get(p).toRealPath());
				data = Util.parseData(convertToRealPath.apply(path));

				analyzer.setCrashRate(crashRate);
				analyzer.setData(data);

				Task<List<Tuple>> analyzeTask = new Task<>() {
					@Override
					protected List<Tuple> call() throws Exception {
						return analyzer.getAnalysisResult();
					}
				};
				analyzeTask.exceptionProperty().addListener((obs, oldValue, newValue) -> newValue.printStackTrace());
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

		root.saveChartBtn.setOnAction(e -> {
			JFXTextField widthInput = root.widthInput;
			JFXTextField heightInput = root.heightInput;

			if (widthInput.validate() & heightInput.validate()) {
				int width = Integer.parseInt(widthInput.getText());
				int height = Integer.parseInt(heightInput.getText());

				FileChooser fileChooser = createFileChooser("Save Chart As Image", "chart.png");
				Optional.ofNullable(fileChooser.showSaveDialog(stage))
					.ifPresent(file -> {
						SaveChartTask saveChartTask = new SaveChartTask(width, height, file, tupleProperty.get(), data);
						saveChartTask.exceptionProperty().addListener((obs, oldValue, newValue) -> newValue.printStackTrace());
						saveChartTask.setOnRunning(event -> root.chartProgressBar.setProgress(-1));
						saveChartTask.setOnSucceeded(event -> root.chartProgressBar.setProgress(0));

						Thread thread = new Thread(saveChartTask);
						thread.setDaemon(true);
						thread.start();
					});
			}
		});
	}

	private FileChooser createFileChooser(String title, String fileName, FileChooser.ExtensionFilter... filters) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.setInitialFileName(fileName);
		fileChooser.getExtensionFilters().addAll(filters);

		return fileChooser;
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
		JFXComboBox<Integer> comboBox = root.comboBox;

		if (comboBox.validate()) {
			int drawnDataNumber = comboBox.getSelectionModel().getSelectedItem();

			Pagination pagination = root.pagination;

			pagination.setPageFactory(i -> {
				Util.updateChart(drawnDataNumber, i, root.chart, newTuples, data, true);
				return root.chart;
			});

			int pageCount = data.size() / drawnDataNumber;
			pageCount = (data.size() % drawnDataNumber == 0) ? pageCount : pageCount + 1;
			pagination.setPageCount(pageCount);

			pagination.setCurrentPageIndex(0);
			pagination.setMaxPageIndicatorCount(5);
		}
	}

	private void updateAnalysisTable(List<Tuple> newTuples) {
		TreeItem<Tuple> rootItem = root.analysisTable.getRoot();
		rootItem.getChildren().clear();

		newTuples.stream()
			.map(TreeItem::new)
			.forEach(rootItem.getChildren()::add);
	}

	private void updateCrashCycleTable(List<Tuple> newTuples) {
		TreeItem<StockPriceCrashCycle> rootItem = root.crashCycleTableWithPeak.getRoot();
		rootItem.getChildren().clear();

		data.stream()
			.map(d -> new TreeItem<StockPriceCrashCycle>(new StockPriceCrashCycle(d, true)))
			.forEach(rootItem.getChildren()::add);
	}

	private static class SaveChartTask extends Task<Void> {
		
		private int width, height;
		private File path;
		private List<Tuple> tuples;
		private List<StockPrice> data;

		private SaveChartTask(int width, int height, File path, List<Tuple> tuples, List<StockPrice> data) {
			this.width = width;
			this.height = height;
			this.path = path;
			this.tuples = tuples;
			this.data = data;
		}

		@Override
		protected Void call() throws Exception {
			// Use shared memory to send the chart data
			FileChannel channel = FileChannel.open(Util.TEMP_FILE,
				StandardOpenOption.READ,
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, Integer.MAX_VALUE);
			sendChartData(buffer);

			// Launch another javafx application to save chart
			// So that the process of rendering chart can't block the main UI thread
			Process process = createProcessBuilder().start();
			process.waitFor();

			return null;
		}

		private ProcessBuilder createProcessBuilder() {
			String javaBinary = getJavaBinaryPath();
			String classpath = System.getProperty("java.class.path");
			String className = ChartSaver.class.getCanonicalName();

			return new ProcessBuilder(javaBinary, "-cp", classpath, className)
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		}

		private String getJavaBinaryPath() {
			String separator = System.getProperty("file.separator");

			StringBuilder javaPath = new StringBuilder();
			javaPath.append(System.getProperty("java.home"))
				.append(separator)
				.append("bin")
				.append(separator)
				.append("java");

			if (isWindows()) {
				javaPath.append(".exe");
			}

			return javaPath.toString();
		}

		private boolean isWindows() {
			return System.getProperty("os.name")
				.toLowerCase()
				.contains("windows");
		}

		private void sendChartData(MappedByteBuffer buffer) throws IOException {
			buffer.putInt(width)
				.putInt(height);

			byte[] pathByteArray = Util.objectToByteArray(path);
			buffer.putInt(pathByteArray.length)
				.put(pathByteArray);

			byte[] tuplesByteArray = Util.objectToByteArray(tuples);
			buffer.putInt(tuplesByteArray.length)
				.put(tuplesByteArray);

			byte[] dataByteArray = Util.objectToByteArray(data);
			buffer.putInt(dataByteArray.length)
				.put(dataByteArray);

			buffer.force();
		}
	}
}
