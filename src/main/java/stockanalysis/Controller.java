package stockanalysis;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
			root.saveBtn.setDisable(newValue.isEmpty());
			root.totalLbl.setText("Total: " + newValue.size());
		});

		root.saveBtn.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save Analysis Result");
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.setInitialFileName("analysis.txt");

			Optional.ofNullable(fileChooser.showSaveDialog(stage))
				.ifPresent(file -> {
					root.progressBar.setProgress(-1);

					new Thread(() -> {
						Util.saveAnalysisResult(tupleProperty.get(), file.toPath());
						Platform.runLater(() -> root.progressBar.setProgress(0));
					}).start();
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

				analyzer.setCrashRate(crashRate);
				analyzer.setPeakDuration(peakDuration);
				analyzer.setPeakDifference(peakDifference);
				analyzer.setPath(path);

				root.progressBar.setProgress(-1);

				new Thread(() -> {
					List<Tuple> tuples = analyzer.getAnalysisResult();

					Platform.runLater(() -> {
						TreeItem<Tuple> rootItem = root.table.getRoot();
						rootItem.getChildren().clear();

						tuples.stream()
							.map(TreeItem::new)
							.forEach(rootItem.getChildren()::add);

						tupleProperty.set(tuples);
						root.progressBar.setProgress(0);
					});
				}).start();
			}
		});
	}
}
