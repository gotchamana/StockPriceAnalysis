package stockanalysis.util;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import static java.nio.file.attribute.PosixFilePermission.*;
import static org.junit.jupiter.api.Assertions.*;
import stockanalysis.model.StockPrice;
import stockanalysis.model.Tuple;

@EnabledIfSystemProperty(named = "class.test", matches = "Util|All")
public class UtilTest {

    @ParameterizedTest 
	@MethodSource("csvPathProvider")
	public void Should_GetSortedStockPrice_When_ParseFromCsvFile(Path path, List<StockPrice> expected) {
		List<StockPrice> data = Util.parseData(path);

		assertEquals(expected, data);
		assertThrows(UnsupportedOperationException.class, () -> data.add(new StockPrice(LocalDate.now(), 0)));
	}

	private static Stream<Arguments> csvPathProvider() {
		Path path1 = Paths.get(UtilTest.class.getResource("/parseDataTest1.csv").getPath());
		Path path2 = Paths.get(UtilTest.class.getResource("/parseDataTest2.csv").getPath());

		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 4), 562.65);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 5), 561.55);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 7), 564.81);

		List<StockPrice> data = Arrays.asList(sp1, sp2, sp3);

		return Stream.of(
				Arguments.of(path1, data),
				Arguments.of(path2, data)
			);
	}

	@Test
	public void Should_GetNullData_When_ParseCsvFileNotExisting() {
		Path path = Paths.get("/foo/bar");
		List<StockPrice> data = Util.parseData(path);

		assertNull(data);
	}

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

	@Test
	public void Should_SaveAnalysisResultToSpecifiedFile() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 1), 500);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 2), 400);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 3), 200);
		Tuple t = new Tuple(sp1, sp2, sp3);

		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		Path dir = fs.getPath("/dir");
		Path path = dir.resolve("analysis.txt");
		Unchecked.<Path>consumer(Files::createDirectory).accept(dir);

		Util.saveAnalysisResult(Arrays.asList(t), path);

		String expected = "Crash Identification Date, Peak Date, Index at Peak, Trough Date, Index at Trough, Peak-to-Trough decline(%), Peak-to-Trough duration(in days)\n" +
			"1980/01/03, 1980/01/01, 500.00, 1980/01/02, 400.00, 20.0, 1\n";
		String actual = Unchecked.<Path, String>function(Files::readString).apply(path);

		assertEquals(expected, actual);
	}

	@EnabledOnOs(OS.LINUX)
	@Test
	public void Should_PrintIOExceptionStacktrace_When_SaveAnalysisResultAtReadOnlyDirectory() {
		StockPrice sp1 = new StockPrice(LocalDate.of(1980, 1, 1), 500);
		StockPrice sp2 = new StockPrice(LocalDate.of(1980, 1, 2), 400);
		StockPrice sp3 = new StockPrice(LocalDate.of(1980, 1, 3), 200);
		Tuple t = new Tuple(sp1, sp2, sp3);

		FileAttribute<?> attr = PosixFilePermissions.asFileAttribute(Set.of(OWNER_READ, GROUP_READ, OTHERS_READ));
		Path dir = Unchecked.<FileAttribute<?>, Path>function(attrs -> Files.createTempDirectory(null, attrs)).apply(attr);
		Path path = dir.resolve("analysis.txt");
		Util.saveAnalysisResult(Arrays.asList(t), path);
	}
}
