package stockanalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Unchecked;

public class Util {

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

	public static void saveAnalysisResult(List<Tuple> tuples, Path path) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY/MM/dd");

		try(BufferedWriter out = Files.newBufferedWriter(path)) {
			out.write("Crash Identification Date, Peak Date, Index at Peak, Trough Date, Index at Trough, Peak-to-Trough decline(%), Peak-to-Trough duration(in days)");
			out.newLine();

			tuples.stream()
				.map(tuple -> String.format("%s, %s, %.2f, %s, %.2f, %.1f, %d",
							tuple.getCrashDate().format(formatter),
							tuple.getPeakDate().format(formatter),
							tuple.getPeakStockPrice(),
							tuple.getTroughDate().format(formatter),
							tuple.getTroughStockPrice(),
							tuple.getPeakTroughDecline(3) * 100,
							tuple.getPeakTroughDuration()))
				.forEach(Unchecked.consumer(line -> {
					out.write(line);
					out.newLine();
				}));

		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
