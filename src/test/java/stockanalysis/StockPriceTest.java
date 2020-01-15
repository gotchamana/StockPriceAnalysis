package stockanalysis;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "class.test", matches = "StockPrice|All")
public class StockPriceTest {

    @ParameterizedTest 
	@MethodSource("stockPriceProvider")
	public void Should_EarlyDateOfStockPriceIsSmallerThanLateDateOfStockPrice_When_TwoStockPriceAreCompared(StockPrice sp1, StockPrice sp2, int expected) {
		assertEquals(expected, sp1.compareTo(sp2));
	}

	private static Stream<Arguments> stockPriceProvider() {
		StockPrice sp1 = new StockPrice(LocalDate.of(2010, 10, 25), 1000);
		StockPrice sp2 = new StockPrice(LocalDate.of(2010, 10, 26), 1000);

		StockPrice sp3 = new StockPrice(LocalDate.of(2010, 10, 25), 2000);
		StockPrice sp4 = new StockPrice(LocalDate.of(2010, 10, 26), 3000);

		return Stream.of(
				Arguments.of(sp1, sp2, -1),
				Arguments.of(sp2, sp1, 1),
				Arguments.of(sp3, sp4, -1),
				Arguments.of(sp4, sp3, 1)
			);
	}
}
