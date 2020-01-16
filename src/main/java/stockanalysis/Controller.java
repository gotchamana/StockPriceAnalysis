package stockanalysis;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.jooq.lambda.Unchecked;

import com.jfoenix.controls.JFXTextField;

public class Controller {
	
    private Analyzer analyzer;
    private ObjectProperty<List<Tuple>> tupleProperty;

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

			// Update table
			TreeItem<Tuple> rootItem = root.table.getRoot();
			rootItem.getChildren().clear();

			newValue.stream()
				.map(TreeItem::new)
				.forEach(rootItem.getChildren()::add);
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
				List<StockPrice> data = Util.parseData(convertToRealPath.apply(path));

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
	}
}
