package stockanalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jooq.lambda.Unchecked;

public class Util {

	private Util() {
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
