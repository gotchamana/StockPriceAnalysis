package stockanalysis;

import java.time.LocalDate;

import lombok.Value;

@Value
public class StockPrice implements Comparable<StockPrice> {
	
	private LocalDate date;
	private double price;

	@Override
	public int compareTo(StockPrice sp) {
		return date.compareTo(sp.getDate());
	}
}
