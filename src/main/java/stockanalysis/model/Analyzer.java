package stockanalysis.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import stockanalysis.util.Util;

@NoArgsConstructor
@AllArgsConstructor
public class Analyzer {
	
	private static final int TOTAL_DAYS_OF_YEAR = 252;
	private static final int LOCAL_RANGE = 90;

	@Setter
	private double crashRate;

	@Setter
	@NonNull
	private List<StockPrice> data;

	public List<Tuple> getAnalysisResult() {
		Set[] lpsAndlts = findLocalPeaksAndLocalTroughs(LOCAL_RANGE, data);
		List<Tuple> tuples = findCandidatePeakCrashPairs(TOTAL_DAYS_OF_YEAR, data);
		tuples = filterSameCandidatePeakTuple(tuples);
		tuples = filterCandidatePeakCrashPairsWithNoLocalTrough(data, tuples, lpsAndlts);
		tuples = findTroughs(data, tuples);

		return tuples;
	}

	private Set[] findLocalPeaksAndLocalTroughs(int range, List<StockPrice> data) {
		Set<StockPrice> localPeaks = new HashSet<>();
		Set<StockPrice> localTroughs = new HashSet<>();

		for (StockPrice sp : data) {
			int spIndex = data.indexOf(sp);
			int from = Math.max(spIndex - range, 0);
			int to = Math.min(spIndex + (range + 1), data.size());
			List<StockPrice> subData = data.subList(from, to);

			if (isPeak(sp, subData)) {
				localPeaks.add(sp);
			}

			if (isTrough(sp, subData)) {
				localTroughs.add(sp);
			}
		}

		return new Set[] { localPeaks, localTroughs };
	}

	private boolean isPeak(StockPrice target, List<StockPrice> data) {
		for (StockPrice sp : data) {
			if (sp.getPrice() > target.getPrice()) {
				return false;
			}
		}

		return true;
	}

	private boolean isTrough(StockPrice target, List<StockPrice> data) {
		for (StockPrice sp : data) {
			if (sp.getPrice() < target.getPrice()) {
				return false;
			}
		}

		return true;
	}

	private List<Tuple> findCandidatePeakCrashPairs(int range, List<StockPrice> data) {
		List<Tuple> tuples = new LinkedList<>();

		for (int i = 1; i < data.size(); i++) {
			int candidatePeakIndex = -1, lastSpLowerThanCurSpIndex = -1;
			StockPrice candidateCrash = data.get(i), candidatePeak = null;

			for (int j = Math.max(i - range, 0); j < i; j++) {
				StockPrice curSp = data.get(j);

				if (candidatePeak == null ||
					candidatePeak.getPrice() <= curSp.getPrice()) {
					candidatePeakIndex = j;
					candidatePeak = curSp;
				}

				lastSpLowerThanCurSpIndex = curSp.getPrice() < candidateCrash.getPrice() ? j : lastSpLowerThanCurSpIndex;
			}

			if (candidatePeakIndex > lastSpLowerThanCurSpIndex &&
				candidatePeak.getPrice() * (1 - crashRate) >= candidateCrash.getPrice()) {
				tuples.add(new Tuple(candidatePeak, null, candidateCrash));
			}
		}
		
		return tuples;
	}

	private List<Tuple> filterSameCandidatePeakTuple(List<Tuple> tuples) {
		Map<StockPrice, List<Tuple>> tuplesByPeak = tuples.stream()
			.collect(Collectors.groupingBy(Tuple::getPeak));

		List<Tuple> rlt = tuplesByPeak.entrySet()
			.stream()
			.map(e -> e.getValue()
				.stream()
				.min(Comparator.comparing(Tuple::getCrashDate))
				.get())
			.collect(Collectors.toList());

		return rlt;
	}

	private List<Tuple> filterCandidatePeakCrashPairsWithNoLocalTrough(List<StockPrice> data, List<Tuple> tuples, Set[] lpsAndlts) {
		List<Tuple> rlt = tuples.stream()
			.filter(t -> hasLocalTrough(t.getPeak(), data, lpsAndlts))
			.collect(Collectors.toList());
		
		return rlt;
	}

	@SuppressWarnings("unchecked")
	private boolean hasLocalTrough(StockPrice peak, List<StockPrice> data, Set[] lpsAndlts) {
		Set<StockPrice> localPeaks = lpsAndlts[0];
		Set<StockPrice> localTroughs = lpsAndlts[1];
		int spIndex = data.indexOf(peak);

		for (int i = spIndex - 1; i >= 0; i--) {
			StockPrice sp = data.get(i);
			if (localTroughs.contains(sp)) {
				return true;
			}

			if (localPeaks.contains(sp)) {
				break;
			}
		}

		return false;
	}

	private List<Tuple> findTroughs(List<StockPrice> data, List<Tuple> tuples) {
		tuples.sort(Comparator.comparing(t -> t.getPeak().getDate()));

		int i = 0;
		for (; i < tuples.size() - 1; i++) {
			int peakIndex1 = data.indexOf(tuples.get(i).getPeak());
			int peakIndex2 = data.indexOf(tuples.get(i + 1).getPeak());
			StockPrice trough = findTroughInRange(peakIndex1 + 1, peakIndex2, data);

			tuples.get(i).setTrough(trough);
		}

		// Last trough will be null, use the default stock price
		tuples.get(i).setTrough(StockPrice.DEFAULT_STOCK_PRICE);

		return tuples;
	}

	private StockPrice findTroughInRange(int from, int to, List<StockPrice> data) {
		return data.subList(from, to)
			.stream()
			.min(Comparator.comparingDouble(StockPrice::getPrice))
			.orElse(StockPrice.DEFAULT_STOCK_PRICE);
	}
}
