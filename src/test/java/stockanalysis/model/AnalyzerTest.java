package stockanalysis.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.fest.reflect.reference.TypeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "class.test", matches = "Analyzer|All")
public class AnalyzerTest {

	private Analyzer analyzer;

	@BeforeEach
	public void init() {
		analyzer = new Analyzer();
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_BePeak_When_AllStockPriceAreLowerThanTarget(List<StockPrice> data) {
		StockPrice target = new StockPrice(LocalDate.of(1980, 1, 8), 700);

		boolean actual = method("isPeak")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(target, data);
		
		assertTrue(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_BeNotPeak_When_SomeStockPriceAreHigherThanTarget(List<StockPrice> data) {
		StockPrice target = new StockPrice(LocalDate.of(1980, 1, 6), 400);

		boolean actual = method("isPeak")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(target, data);
		
		assertFalse(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_BeTrough_When_AllStockPriceAreHigherThanTarget(List<StockPrice> data) {
		StockPrice target = new StockPrice(LocalDate.of(1980, 1, 5), 200);

		boolean actual = method("isTrough")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(target, data);
		
		assertTrue(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_BeNotTrough_When_SomeStockPriceAreLowerThanTarget(List<StockPrice> data) {
		StockPrice target = new StockPrice(LocalDate.of(1980, 1, 4), 600);

		boolean actual = method("isTrough")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(target, data);
		
		assertFalse(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	@SuppressWarnings("unchecked")
	public void Should_GetAllLocalPeaksAndLocalTroughs(List<StockPrice> data) {
		Set<StockPrice> expectedLocalPeaks = Set.of(data.get(0), data.get(2), data.get(4));
		Set<StockPrice> expectedLocalTroughs = Set.of(data.get(1), data.get(3));

		Set[] actual = method("findLocalPeaksAndLocalTroughs")
			.withReturnType(new TypeRef<Set[]>() {})
			.withParameterTypes(int.class, List.class)
			.in(analyzer)
			.invoke(1, data);
		
		Set<StockPrice> actualLocalPeaks = actual[0];
		Set<StockPrice> actualLocalTroughs = actual[1];

		assertThat(actualLocalPeaks).hasSameElementsAs(expectedLocalPeaks);
		assertThat(actualLocalTroughs).hasSameElementsAs(expectedLocalTroughs);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_GetCandidatePeakCrashPairs(List<StockPrice> data) {
		analyzer.setCrashRate(0.1);

		List<Tuple> expected = Arrays.asList(new Tuple(data.get(0), null, data.get(1)));
		List<Tuple> actual = method("findCandidatePeakCrashPairs")
			.withReturnType(new TypeRef<List<Tuple>>() {})
			.withParameterTypes(int.class, List.class)
			.in(analyzer)
			.invoke(2, data);
		
		assertThat(actual).containsExactlyElementsOf(expected);
	}

	@Test
	public void Should_FilterTuplesHavingTheSameCandidatePeak() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 200);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 400);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 100);

		Tuple t1 = new Tuple(sp1, null, sp2);
		Tuple t2 = new Tuple(sp1, null, sp3);
		Tuple t3 = new Tuple(sp3, null, sp4);

		List<Tuple> tuples = Arrays.asList(t1, t2, t3);

		List<Tuple> expected = Arrays.asList(t1, t3);
		List<Tuple> actual = method("filterSameCandidatePeakTuple")
			.withReturnType(new TypeRef<List<Tuple>>() {})
			.withParameterTypes(List.class)
			.in(analyzer)
			.invoke(tuples);
		
		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_HaveLocalTrough(List<StockPrice> data) {
		StockPrice peak = data.get(4);
		Set[] lpsAndlts = new Set[] { Set.of(data.get(0)), Set.of(data.get(1)) };

		boolean actual = method("hasLocalTrough")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class, Set[].class)
			.in(analyzer)
			.invoke(peak, data, lpsAndlts);

		assertTrue(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_NotHaveLocalTrough_When_EncounterLocalPeak(List<StockPrice> data) {
		StockPrice peak = data.get(4);
		Set[] lpsAndlts = new Set[] { Set.of(data.get(2)), Set.of(data.get(1)) };

		boolean actual = method("hasLocalTrough")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class, Set[].class)
			.in(analyzer)
			.invoke(peak, data, lpsAndlts);

		assertFalse(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_NotHaveLocalTrough_When_NotEncounterAnyLocalTrough(List<StockPrice> data) {
		StockPrice peak = data.get(1);
		Set[] lpsAndlts = new Set[] { Set.of(data.get(4)), Set.of() };

		boolean actual = method("hasLocalTrough")
			.withReturnType(new TypeRef<Boolean>() {})
			.withParameterTypes(StockPrice.class, List.class, Set[].class)
			.in(analyzer)
			.invoke(peak, data, lpsAndlts);

		assertFalse(actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_FilterTuplesWithoutLocalTrough(List<StockPrice> data) {
		Set[] lpsAndlts = new Set[] { Set.of(data.get(0), data.get(3)), Set.of(data.get(1)) };

		Tuple t1 = new Tuple(data.get(0), null, data.get(1));
		Tuple t2 = new Tuple(data.get(1), null, data.get(2));
		Tuple t3 = new Tuple(data.get(2), null, data.get(3));

		List<Tuple> tuples = Arrays.asList(t1, t2, t3);

		List<Tuple> expected = Arrays.asList(t3);
		List<Tuple> actual = method("filterCandidatePeakCrashPairsWithNoLocalTrough")
			.withReturnType(new TypeRef<List<Tuple>>() {})
			.withParameterTypes(List.class, List.class, Set[].class)
			.in(analyzer)
			.invoke(data, tuples, lpsAndlts);

		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_FindTrough(List<StockPrice> data) {
		StockPrice expected = data.get(2);
		StockPrice actual = method("findTroughInRange")
			.withReturnType(new TypeRef<StockPrice>() {})
			.withParameterTypes(int.class, int.class, List.class)
			.in(analyzer)
			.invoke(2, 5, data);

		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_PopulateTupleWithTrough(List<StockPrice> data) {
		Tuple t1 = new Tuple(data.get(0), null, data.get(1));
		Tuple t2 = new Tuple(data.get(2), null, data.get(3));
		Tuple t3 = new Tuple(data.get(4), null, data.get(4));

		Tuple e1 = new Tuple(data.get(0), data.get(1), data.get(1));
		Tuple e2 = new Tuple(data.get(2), data.get(3), data.get(3));

		List<Tuple> tuples = Arrays.asList(t2, t1, t3);

		List<Tuple> expected = Arrays.asList(e1, e2, t3);
		List<Tuple> actual = method("findTroughs")
			.withReturnType(new TypeRef<List<Tuple>>() {})
			.withParameterTypes(List.class, List.class)
			.in(analyzer)
			.invoke(data, tuples);

		assertThat(actual).containsExactlyElementsOf(expected);
	}

	@ParameterizedTest
	@MethodSource("dataProvider")
	public void Should_GetCorrectAnalysisResult(List<StockPrice> data) {
		analyzer.setCrashRate(0.1);
		analyzer.setData(data);

		List<Tuple> expected = Arrays.asList();
		List<Tuple> actual = analyzer.getAnalysisResult();

		assertThat(actual).containsExactlyElementsOf(expected);
	}

	private static Stream<Arguments> dataProvider() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 200);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 400);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 400);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 8), 700);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3, sp4, sp5);

		return Stream.of(Arguments.of(data));
	}
}
