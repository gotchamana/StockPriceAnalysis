package stockanalysis;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class Tuple extends RecursiveTreeObject<Tuple> implements Cloneable, Serializable {

	private static final long serialVersionUID = 95438933L;

	private StockPrice peak;
	private StockPrice trough;
	private StockPrice crash;

	public LocalDate getPeakDate() {
		return peak.getDate();
	}

	public LocalDate getTroughDate() {
		return trough.getDate();
	}

	public LocalDate getCrashDate() {
		return crash.getDate();
	}

	public double getPeakStockPrice() {
		return peak.getPrice();
	}

	public double getTroughStockPrice() {
		return trough.getPrice();
	}

	public double getPeakTroughDecline(int decimalPlace) {
		double decline = (peak.getPrice() - trough.getPrice()) / peak.getPrice();
		double shift = Math.pow(10, decimalPlace);
		decline = Math.round(decline * shift) / shift;

		return decline;
	}

	public int getPeakTroughDuration() {
		LocalDateTime peakDateTime = peak.getDate().atStartOfDay();
		LocalDateTime troughDateTime = trough.getDate().atStartOfDay();

		return Util.calcTwoDateDurationInDays(peakDateTime, troughDateTime);
	}

	@Override
	public Object clone() {
		return new Tuple(peak, trough, crash);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
