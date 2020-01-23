package stockanalysis.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.jooq.lambda.Unchecked;

import stockanalysis.model.StockPrice;
import stockanalysis.model.Tuple;

public class ChartSaver extends Application {

	private int width, height;
	private File path;
	private List<Tuple> tuples;
	private List<StockPrice> data;

	@Override
	public void start(Stage primaryStage) {
		Unchecked.runnable(() -> {
			// Use shared memory to receive chart data from main process
			FileChannel channel = FileChannel.open(Util.TEMP_FILE, StandardOpenOption.READ);
			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, Integer.MAX_VALUE);
			receiveChartData(buffer);

			LineChart<String, Number> chart = Util.createLineChart();
			Util.updateChart(data.size(), 0, chart, tuples, data, false);

			Scene scene = new Scene(chart, width, height);
			scene.getStylesheets().add("/style.css");
			saveChart(scene);
		}).run();

		Platform.exit();
	}

	@SuppressWarnings("unchecked")
	private void receiveChartData(MappedByteBuffer buffer) throws Exception {
		width = buffer.getInt();
		height = buffer.getInt();

		byte[] pathByteArray = new byte[buffer.getInt()];
		buffer.get(pathByteArray);
		path = Util.byteArrayToObject(pathByteArray, File.class);

		byte[] tuplesByteArray = new byte[buffer.getInt()];
		buffer.get(tuplesByteArray);
		tuples = Util.byteArrayToObject(tuplesByteArray, List.class);

		byte[] dataByteArray = new byte[buffer.getInt()];
		buffer.get(dataByteArray);
		data = Util.byteArrayToObject(dataByteArray, List.class);
	}

	private void saveChart(Scene scene) throws IOException {
		WritableImage wi = scene.snapshot(null);
		BufferedImage bi = SwingFXUtils.fromFXImage(wi, null);
		ImageIO.write(bi, "PNG", path);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
