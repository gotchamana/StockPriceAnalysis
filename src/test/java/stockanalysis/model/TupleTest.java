package stockanalysis.model;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "class.test", matches = "Tuple|All")
public class TupleTest {

    @ParameterizedTest 
	@MethodSource("tupleDurationProvider")
	public void Should_GetCorrectDurationInDays_When_CalculateDurationBetweenPeakAndTrough(Tuple tuple, int expected) {
		assertEquals(expected, tuple.getPeakTroughDuration());
	}

	private static Stream<Arguments> tupleDurationProvider() {
		StockPrice crash = new StockPrice(LocalDate.now(), 0);

		StockPrice peak1 = new StockPrice(LocalDate.of(1971, 4, 28), 0);
		StockPrice trough1 = new StockPrice(LocalDate.of(1971, 11, 23), 0);
		Tuple tuple1 = new Tuple(peak1, trough1, crash);

		StockPrice peak2 = new StockPrice(LocalDate.of(1973, 1, 11), 0);
		StockPrice trough2 = new StockPrice(LocalDate.of(1974, 10, 3), 0);
		Tuple tuple2 = new Tuple(peak2, trough2, crash);

		StockPrice peak3 = new StockPrice(LocalDate.of(1980, 2, 13), 0);
		StockPrice trough3 = new StockPrice(LocalDate.of(1980, 3, 27), 0);
		Tuple tuple3 = new Tuple(peak3, trough3, crash);

		StockPrice peak4 = new StockPrice(LocalDate.of(1983, 10, 10), 0);
		StockPrice trough4 = new StockPrice(LocalDate.of(1984, 7, 24), 0);
		Tuple tuple4 = new Tuple(peak4, trough4, crash);

		return Stream.of(
				Arguments.of(tuple1, 209),
				Arguments.of(tuple2, 630),
				Arguments.of(tuple3, 43),
				Arguments.of(tuple4, 288)
			);
	}
}
