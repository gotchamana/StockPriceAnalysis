package stockanalysis.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Circle;
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
			// Use Internet to receive chart data from main process
			Socket socket = createSocket();
			receiveChartData(socket.getInputStream());

			LineChart<String, Number> chart = createChart();
			updateChart(data.size(), 0, chart, tuples);

			Scene scene = new Scene(chart, width, height);
			scene.getStylesheets().add("/style.css");

			saveChart(scene);
		}).run();

		Platform.exit();
	}

	private Socket createSocket() throws IOException {
		InetAddress localhost = InetAddress.getLocalHost();
		int port = 9487;

		return new Socket(localhost, port);
	}

	@SuppressWarnings("unchecked")
	private void receiveChartData(InputStream ssin) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(ssin));

		try(in) {
			width = in.readInt();
			height = in.readInt();
			path = (File) in.readObject();
			tuples = (List) in.readObject();
			data = (List) in.readObject();
		}
	}

	private LineChart<String, Number> createChart() {
		CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();

		LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
		chart.setAnimated(false);
		chart.setCreateSymbols(false);
		chart.getData().add(new XYChart.Series<>());

		return chart;
	}

	@SuppressWarnings("unchecked")
	private void updateChart(int drawnDataNumber, int skipFactor, LineChart<String, Number> chart, List<Tuple> newTuples) {
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
					Node symbol = createSymbol("Peak");
					spData.setNode(symbol);
				} else if (peakTroughs[1].contains(sp)) {
					Node symbol = createSymbol("Trough");
					spData.setNode(symbol);
				}
			})
			.collect(Collectors.toList());

		chart.getData()
			.get(0)
			.getData()
			.setAll(chartData);
	}

	private Node createSymbol(String type) {
		Circle symbol = new Circle(5);
		symbol.getStyleClass().addAll("chart-symbol", "chart-" + type.toLowerCase());

		return symbol;
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
