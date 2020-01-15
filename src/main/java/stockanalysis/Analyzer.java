package stockanalysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class Analyzer {
	
	@Setter
	private double crashRate;

	@Setter
	private int peakDuration;

	@Setter
	private double peakDifference;

	@Setter
	@NonNull
	private String path;

	public List<Tuple> getAnalysisResult() {
		List<Tuple> tuples = null;

		try {
			List<StockPrice> data = Util.parseData(Paths.get(path).toRealPath());
			tuples = firstProcess(90, data);
			tuples = secondProcess(tuples, data);

		} catch(IOException e){
			e.printStackTrace();
		}

		return tuples;
	}

	private List<Tuple> firstProcess(int range, List<StockPrice> data) {
		List<Tuple> rlt = new ArrayList<>();

		for (StockPrice sp : data) {
			int spIndex = data.indexOf(sp);
			int from = Math.max(spIndex - range, 0);
			int to = Math.min(spIndex + (range + 1), data.size());

			StockPrice peak = getPeakInRange(from, to, data);
			getCrashIdentificationOfPeak(peak, data).ifPresent(crash -> {
				rlt.add(new Tuple(peak, null, crash));
			});
		}

		return rlt;
	}

	private StockPrice getPeakInRange(int from, int to, List<StockPrice> data) {
		return data.subList(from, to)
			.stream()
			.max(Comparator.comparingDouble(StockPrice::getPrice))
			.get();
	}

	private Optional<StockPrice> getCrashIdentificationOfPeak(StockPrice peak, List<StockPrice> data) {
		return data.stream()
			.skip(data.indexOf(peak) + 1)
			.dropWhile(sp -> sp.getPrice() > peak.getPrice() * (1 - crashRate))
			.findFirst();
	}

	private List<Tuple> secondProcess(List<Tuple> tuples, List<StockPrice> data) {
		if (tuples.isEmpty()) {
			return tuples;
		}

		List<Tuple> copyTuples = tuples.stream()
			.map(tuple -> (Tuple) tuple.clone())
			.collect(Collectors.toList());

		Deque<Integer> adoptTupleIndices = new LinkedList<>();
		adoptTupleIndices.add(0);

		copyTuples.stream()
			.skip(1)
			.forEach(curTuple -> {
				Tuple preTuple = copyTuples.get(adoptTupleIndices.getLast());

				StockPrice prePeak = preTuple.getPeak();
				StockPrice curPeak = curTuple.getPeak();

				LocalDateTime prePeakDateTime = prePeak.getDate().atStartOfDay();
				LocalDateTime curPeakDateTime = curPeak.getDate().atStartOfDay();

				double prePeakPrice = prePeak.getPrice();
				double curPeakPrice = curPeak.getPrice();

				int duration = Util.calcTwoDateDurationInDays(prePeakDateTime, curPeakDateTime);
				double priceDiff = Math.abs(prePeakPrice - curPeakPrice) / prePeakPrice;

				if (duration >= peakDuration && priceDiff >= peakDifference) {
					int prePeakIndex = data.indexOf(prePeak);
					int curPeakIndex = data.indexOf(curPeak);
					StockPrice trough = getTroughInRange(prePeakIndex, curPeakIndex + 1, data);

					if (trough.getPrice() < Math.min(prePeakPrice, curPeakPrice)) {
						preTuple.setTrough(trough);
						adoptTupleIndices.add(copyTuples.indexOf(curTuple));
					}
				}
			});

		return adoptTupleIndices.stream()
			.map(index -> copyTuples.get(index))
			.takeWhile(tuple -> tuple.getTrough() != null)
			.collect(Collectors.toList());
	}

	private StockPrice getTroughInRange(int from, int to, List<StockPrice> data) {
		List<StockPrice> copySubList = new ArrayList<>(data.subList(from, to));
		Collections.reverse(copySubList);

		return Collections.min(copySubList, Comparator.comparingDouble(StockPrice::getPrice));
	}
}
