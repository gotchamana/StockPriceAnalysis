package stockanalysis;

import java.time.LocalDateTime;
import java.time.Duration;

public class Util {

	private Util() {
	}

	public static int calcTwoDateDurationInDays(LocalDateTime from, LocalDateTime to) {
		Duration duration = Duration.between(from, to);
		return (int) (duration.getSeconds() / 60 / 60 / 24);
	}
}
