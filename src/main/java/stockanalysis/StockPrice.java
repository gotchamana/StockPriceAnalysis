package stockanalysis;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Value;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

@Value
public class StockPrice implements Comparable<StockPrice>, Serializable {
	
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
