package stockanalysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "class.test", matches = "Util")
public class UtilTest {

    @ParameterizedTest 
	@MethodSource("dateTimeProvider")
	public void Should_GetTheDurationInDaysBetweenTwoDateTime(LocalDateTime from, LocalDateTime to, int expected) {
		int days = Util.calcTwoDateDurationInDays(from, to);

		assertEquals(expected, days);
	}

	private static Stream<Arguments> dateTimeProvider() {
		LocalDateTime l1 = LocalDate.of(2004, 1, 1).atStartOfDay();
		LocalDateTime l2 = LocalDate.of(2004, 1, 2).atStartOfDay();
		LocalDateTime l3 = LocalDate.of(2005, 1, 1).atStartOfDay();

		return Stream.of(
				Arguments.of(l1, l1, 0),
				Arguments.of(l1, l2, 1),
				Arguments.of(l1, l3, 366)
			);
	}
}
