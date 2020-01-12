package stockanalysis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.fest.reflect.reference.TypeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.fest.reflect.core.Reflection.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "class.test", matches = "Analyzer")
public class AnalyzerTest {

	private Analyzer analyzer;

	@BeforeEach
	public void init() {
		analyzer = new Analyzer();
	}

    @ParameterizedTest 
	@MethodSource("csvPathProvider")
	public void Should_GetSortedStockPrice_When_ParseFromCsvFile(Path path, List<StockPrice> expected) {
		List<StockPrice> data = method("parseData")
			.withReturnType(new TypeRef<List<StockPrice>>() {})
			.withParameterTypes(Path.class)
			.in(analyzer)
			.invoke(path);

		assertEquals(expected, data);
		assertThrows(UnsupportedOperationException.class, () -> data.add(new StockPrice(LocalDate.now(), 0)));
	}

	private static Stream<Arguments> csvPathProvider() {
		Path path1 = Paths.get(AnalyzerTest.class.getResource("/parseDataTest1.csv").getPath());
		Path path2 = Paths.get(AnalyzerTest.class.getResource("/parseDataTest2.csv").getPath());

		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 562.65);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 561.55);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 7), 564.81);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3);

		return Stream.of(
				Arguments.of(path1, data),
				Arguments.of(path2, data)
			);
	}

    @ParameterizedTest 
	@MethodSource("dataPeakProvider")
	public void Should_GetThePeakInTheFirstOccurenceWithTheRange(int from, int to, List<StockPrice> data, StockPrice expected) {
		StockPrice peak = method("getPeakInRange")
			.withReturnType(new TypeRef<StockPrice>() {})
			.withParameterTypes(int.class, int.class, List.class)
			.in(analyzer)
			.invoke(from, to, data);

		assertEquals(expected, peak);
	}

	private static Stream<Arguments> dataPeakProvider() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 200);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 400);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 300);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 7), 400);
		StockPrice sp6 = new StockPrice(LocalDate.of(1980, 1, 8), 700);

		List<StockPrice> data1 = Arrays.asList(sp1, sp2, sp3, sp4, sp6);
		List<StockPrice> data2 = Arrays.asList(sp1, sp2, sp3, sp5, sp6);

		return Stream.of(
				Arguments.of(1, 4, data1, sp3),
				Arguments.of(1, 4, data2, sp3)
			);
	}

    @Test 
	public void Should_ReturnEmptyOptional_When_CannotFindACrashIdentification() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 700);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 900);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 800);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 8), 1000);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3, sp4, sp5);

		analyzer.setCrashRate(0.1);

		Optional<StockPrice> crash = method("getCrashIdentificationOfPeak")
			.withReturnType(new TypeRef<Optional<StockPrice>>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(sp2, data);

		assertEquals(Optional.empty(), crash);
	}

    @Test 
	public void Should_GetTheFirstCrashIdentificationAccrodingToCrashRate() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 700);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 900);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 600);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 8), 500);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3, sp4, sp5);

		analyzer.setCrashRate(0.1);

		Optional<StockPrice> crash = method("getCrashIdentificationOfPeak")
			.withReturnType(new TypeRef<Optional<StockPrice>>() {})
			.withParameterTypes(StockPrice.class, List.class)
			.in(analyzer)
			.invoke(sp2, data);

		assertEquals(sp4, crash.get());
	}

	@Test
	public void Should_GetTheCorrectResultOfFirstProcess() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 500);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 700);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 600);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 800);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 8), 400);
		StockPrice sp6 = new StockPrice(LocalDate.of(1980, 1, 9), 900);
		StockPrice sp7 = new StockPrice(LocalDate.of(1980, 1, 10), 100);
		StockPrice sp8 = new StockPrice(LocalDate.of(1980, 1, 11), 300);

		Tuple t1 = new Tuple(sp2, null, sp3);
		Tuple t2 = new Tuple(sp2, null, sp3);
		Tuple t3 = new Tuple(sp4, null, sp5);
		Tuple t4 = new Tuple(sp4, null, sp5);
		Tuple t5 = new Tuple(sp6, null, sp7);
		Tuple t6 = new Tuple(sp6, null, sp7);
		Tuple t7 = new Tuple(sp6, null, sp7);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3, sp4, sp5, sp6, sp7);
		List<Tuple> expected = Arrays.asList(t1, t2, t3, t4, t5, t6, t7);

		analyzer.setCrashRate(0.1);

		List<StockPrice> firstProcessResult = method("firstProcess")
			.withReturnType(new TypeRef<List<StockPrice>>() {})
			.withParameterTypes(int.class, List.class)
			.in(analyzer)
			.invoke(1, data);

		assertEquals(expected, firstProcessResult);
	}

    @ParameterizedTest 
	@MethodSource("dataTroughProvider")
	public void Should_GetTheTroughInTheLastOccurenceWithTheRange(int from, int to, List<StockPrice> data, StockPrice expected) {
		StockPrice trough = method("getTroughInRange")
			.withReturnType(new TypeRef<StockPrice>() {})
			.withParameterTypes(int.class, int.class, List.class)
			.in(analyzer)
			.invoke(from, to, data);

		assertEquals(expected, trough);
	}

	private static Stream<Arguments> dataTroughProvider() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 600);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 200);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 400);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 6), 200);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 7), 200);
		StockPrice sp6 = new StockPrice(LocalDate.of(1980, 1, 7), 400);
		StockPrice sp7 = new StockPrice(LocalDate.of(1980, 1, 8), 700);

		List<StockPrice> data1 = Arrays.asList(sp1, sp2, sp3, sp5, sp7);
		List<StockPrice> data2 = Arrays.asList(sp1, sp2, sp4, sp6, sp7);

		return Stream.of(
				Arguments.of(0, 5, data1, sp5),
				Arguments.of(0, 5, data2, sp4)
			);
	}

    @ParameterizedTest 
	@MethodSource("secondProcessProvider")
	public void Should_GetTheCorrectResultOfSecondProcess(int peakDuration, double peakDifference, List<Tuple> tuples, List<StockPrice> data, List<Tuple> expected) {
		analyzer.setPeakDuration(peakDuration);
		analyzer.setPeakDifference(peakDifference);

		List<StockPrice> secondProcessResult = method("secondProcess")
			.withReturnType(new TypeRef<List<StockPrice>>() {})
			.withParameterTypes(List.class, List.class)
			.in(analyzer)
			.invoke(tuples, data);

		assertEquals(expected, secondProcessResult);
	}

	private static Stream<Arguments> secondProcessProvider() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 900);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 700);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 6), 800);
		StockPrice sp4 = new StockPrice(LocalDate.of(1980, 1, 7), 400);
		StockPrice sp5 = new StockPrice(LocalDate.of(1980, 1, 8), 1000);
		StockPrice sp6 = new StockPrice(LocalDate.of(1980, 1, 9), 700);
		StockPrice sp7 = new StockPrice(LocalDate.of(1980, 1, 10), 1200);
		StockPrice sp8 = new StockPrice(LocalDate.of(1980, 1, 10), 800);

		Tuple t1 = new Tuple(sp1, null, null);
		Tuple t2 = new Tuple(sp3, null, null);
		Tuple t3 = new Tuple(sp5, null, null);
		Tuple t4 = new Tuple(sp6, null, null);
		Tuple t5 = new Tuple(sp7, null, null);
		Tuple t6 = new Tuple(sp8, null, null);

		Tuple t1Copy1 = new Tuple(sp1, sp2, null);
		Tuple t1Copy2 = new Tuple(sp1, sp4, null);
		Tuple t2Copy1 = new Tuple(sp3, sp4, null);
		Tuple t3Copy1 = new Tuple(sp5, sp6, null);

		List<Tuple> tuples1 = Arrays.asList(t1, t2, t3);
		List<StockPrice> data1 = Arrays.asList(sp1, sp2, sp3, sp4, sp5);
		List<Tuple> expected1 = Arrays.asList(t1Copy1, t2Copy1);

		List<Tuple> tuples2 = Arrays.asList(t1, t2, t3, t4, t5);
		List<StockPrice> data2 = Arrays.asList(sp1, sp2, sp3, sp4, sp5, sp6, sp7);
		List<Tuple> expected2 = Arrays.asList(t1Copy1, t2Copy1, t3Copy1);

		List<Tuple> tuples3 = Arrays.asList(t1, t2, t3, t4, t6);
		List<StockPrice> data3 = Arrays.asList(sp1, sp2, sp3, sp4, sp5, sp6, sp8);
		List<Tuple> expected3 = Arrays.asList(t1Copy2);

		return Stream.of(
				Arguments.of(1, 0.1, tuples1, data1, expected1),
				Arguments.of(1, 0.1, tuples2, data2, expected2),
				Arguments.of(1, 0.2, tuples2, data2, expected3),
				Arguments.of(5, 0.2, tuples3, data3, expected3)
			);
	}
}
