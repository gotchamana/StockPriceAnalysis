package stockanalysis.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;

import org.jooq.lambda.Unchecked;

import stockanalysis.model.StockPrice;
import stockanalysis.model.Tuple;

public class Util {

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("YYYY/MM/dd");
	public static final Path TEMP_FILE = Paths.get(System.getProperty("java.io.tmpdir")).resolve("StockAnalysis.tmp");

	private Util() {
	}

	public static List<StockPrice> parseData(Path file) {
		try(Stream<String> lines = Files.lines(file)) {
			return lines.skip(1)
				.map(line -> line.split("\\s*,\\s*"))
				.map(line -> {
					LocalDate date = LocalDate.parse(line[0], DateTimeFormatter.ofPattern("yyyy/M/d"));
					double price = Double.parseDouble(line[1]);
					return new StockPrice(date, price);
				})
				.sorted()
				.collect(Collectors.toUnmodifiableList());

		} catch(IOException e) {
			e.printStackTrace();
		}	

		return null;
	}

	public static int calcTwoDateDurationInDays(LocalDateTime from, LocalDateTime to) {
		Duration duration = Duration.between(from, to);
		return (int) (duration.getSeconds() / 60 / 60 / 24);
	}

	public static boolean checkDateIsBetween(LocalDate from, LocalDate to, LocalDate target) {
		// Inclusive
		return (target.equals(from) || target.equals(to)) || (target.isAfter(from) && target.isBefore(to));
	}

	public static void saveAnalysisResult(List<Tuple> tuples, Path path) {
		try(BufferedWriter out = Files.newBufferedWriter(path)) {
			out.write("Crash Identification Date, Peak Date, Index at Peak, Trough Date, Index at Trough, Peak-to-Trough decline(%), Peak-to-Trough duration(in days)");
			out.newLine();

			tuples.stream()
				.map(tuple -> String.format("%s, %s, %.2f, %s, %.2f, %.1f, %d",
							tuple.getCrashDate().format(DATE_FORMATTER),
							tuple.getPeakDate().format(DATE_FORMATTER),
							tuple.getPeakStockPrice(),
							tuple.getTroughDate().format(DATE_FORMATTER),
							tuple.getTroughStockPrice(),
							tuple.getPeakTroughDecline() * 100,
							tuple.getPeakTroughDuration()))
				.forEach(Unchecked.consumer(line -> {
					out.write(line);
					out.newLine();
				}));

		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static LineChart<String, Number> createLineChart() {
		CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();

		LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
		chart.setAnimated(false);
		chart.getData().add(new XYChart.Series<>());

		return chart;
	}

	public static void updateChart(int drawnDataNumber, int skipFactor, LineChart<String, Number> chart, List<Tuple> newTuples, List<StockPrice> data, boolean tooltip) {
		Set[] peakTroughs = groupPeaksTroughs(newTuples);
		List<XYChart.Data<String, Number>> chartData = convertToXYData(drawnDataNumber, skipFactor, data);

		addSymbols(peakTroughs, chartData, tooltip);

		chart.getData()
			.get(0)
			.getData()
			.setAll(chartData);
	}

	private static Set[] groupPeaksTroughs(List<Tuple> tuples) {
		Set<StockPrice> peaks = new HashSet<>();
		Set<StockPrice> troughs = new HashSet<>();

		tuples.forEach(tuple -> {
			peaks.add(tuple.getPeak());
			troughs.add(tuple.getTrough());
		});

		return new Set[]{ peaks, troughs };
	}

	private static List<XYChart.Data<String, Number>> convertToXYData(int drawnDataNumber, int skipFactor, List<StockPrice> data) {
		List<XYChart.Data<String, Number>> chartData = data.stream()
			.skip(drawnDataNumber * skipFactor)
			.limit(drawnDataNumber)
			.map(sp -> {
				String date = sp.getDate().format(Util.DATE_FORMATTER);
				Number price = sp.getPrice();

				return new XYChart.Data<>(date, price, sp);
			})
			.collect(Collectors.toList());

		return chartData;
	}

	private static void addSymbols(Set[] peakTroughs, List<XYChart.Data<String, Number>> chartData, boolean tooltip) {
		chartData.forEach(spData -> {
			StockPrice sp = (StockPrice) spData.getExtraValue();
			Node symbol;

			if (peakTroughs[0].contains(sp)) {
				symbol = createSymbol(StockPrice.Type.PEAK, sp, tooltip);
			} else if (peakTroughs[1].contains(sp)) {
				symbol = createSymbol(StockPrice.Type.TROUGH, sp, tooltip);
			} else {
				symbol = createSymbol(StockPrice.Type.NONE, sp, tooltip);
			}

			spData.setNode(symbol);
		});
	}

	private static Node createSymbol(StockPrice.Type type, StockPrice sp, boolean tooltip) {
		if (type.equals(StockPrice.Type.NONE)) {
			return new Group();
		}

		Circle symbol = new Circle(5);
		symbol.setOnMouseEntered(e -> symbol.setRadius(6));
		symbol.setOnMouseExited(e -> symbol.setRadius(5));
		symbol.getStyleClass().addAll("chart-symbol", "chart-" + type.toString().toLowerCase());

		if (tooltip) {
			installTooltips(symbol, type, sp);
		}

		return symbol;
	}

	private static void installTooltips(Node symbol, StockPrice.Type type, StockPrice sp) {
		Tooltip tooltip = new Tooltip(String.format("%s: %s%n%s: %s%n%s: %.2f",
			"Type", type.toString(),
			"Date", sp.getDate().format(DATE_FORMATTER),
			"Price", sp.getPrice()));
		Tooltip.install(symbol, tooltip);
	}

	public static <T> byte[] objectToByteArray(T obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(baos));
		out.writeObject(obj);
		out.flush();

		return baos.toByteArray();
	}

	@SuppressWarnings("unchecked")
	public static <T> T byteArrayToObject(byte[] array, Class<T> clazz) throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(array)));
		return (T) in.readObject();
	}
}
