package stockanalysis.control;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.InterruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import org.jooq.lambda.Unchecked;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import stockanalysis.model.Analyzer;
import stockanalysis.model.StockPrice;
import stockanalysis.model.Tuple;
import stockanalysis.util.ChartSaver;
import stockanalysis.util.Util;
import stockanalysis.view.StockAnalysisPane;
import javafx.scene.Group;

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

			updateTable(newValue);
		});

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

	private void updateTable(List<Tuple> newTuples) {
		TreeItem<Tuple> rootItem = root.table.getRoot();
		rootItem.getChildren().clear();

		newTuples.stream()
			.map(TreeItem::new)
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
			// Launch another javafx application to save chart
			// So that the process of rendering chart can't block the main UI thread
			Process process = createProcessBuilder().start();

			// Use Internet to send the chart data
			ServerSocket serverSocket = createServerSocket();
			Socket socket = serverSocket.accept();
			sendChartData(socket.getOutputStream());

			process.waitFor();

			return null;
		}

		private ServerSocket createServerSocket() throws Exception {
			int port = 9487;
			int backlog = 1;
			InetAddress localhost = InetAddress.getLocalHost();

			return new ServerSocket(port, backlog, localhost);
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

		private void sendChartData(OutputStream sout) throws IOException {
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(sout));

			try(out) {
				out.writeInt(width);
				out.writeInt(height);
				out.writeObject(path);
				out.writeObject(tuples);
				out.writeObject(data);
			}
		}
	}
}
