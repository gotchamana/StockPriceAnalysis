package stockanalysis.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.Value;

@Value
public class StockPrice implements Comparable<StockPrice>, Serializable {
	
	public static enum Type {
		NONE, PEAK, TROUGH;

		@Override
		public String toString() {
			return capitalizeFirstLetter(super.toString().toLowerCase());
		}

		private String capitalizeFirstLetter(String str) {
			if (str.isEmpty()) {
				return str;
			}

			StringBuilder sb = new StringBuilder(str);
			char capitalLetter = Character.toUpperCase(sb.charAt(0));
			sb.setCharAt(0, capitalLetter);

			return sb.toString();
		}
	}

	public static final StockPrice DEFAULT_STOCK_PRICE = new StockPrice(LocalDate.MAX, Double.POSITIVE_INFINITY);

	private static final long serialVersionUID = 43204820L;

	private LocalDate date;
	private double price;

	@Override
	public int compareTo(StockPrice sp) {
		return date.compareTo(sp.getDate());
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
