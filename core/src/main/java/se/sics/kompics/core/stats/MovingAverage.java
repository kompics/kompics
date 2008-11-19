package se.sics.kompics.core.stats;

public class MovingAverage {

	private double[] recentData;

	private int first;

	private int size;

	private int count;

	private double sum;

	public MovingAverage(int size) {
		this.size = size;
		recentData = new double[size];
		this.first = 0;
		this.sum = 0;
		this.count = 0;
	}

	// data.length < recentData
	public void pushData(double[] data) {
		if (count < size) {
			System.arraycopy(data, 0, recentData, first, data.length);
			first += data.length;
			first %= size;
			count += data.length;
			for (int i = 0; i < data.length; i++) {
				sum += data[i];
			}
		} else {
			for (int i = first; i < first + data.length; i++) {
				sum -= recentData[i];
			}

			System.arraycopy(data, 0, recentData, first, data.length);

			first += data.length;
			first %= size;
			for (int i = 0; i < data.length; i++) {
				sum += data[i];
			}
		}
	}

	public double getMovingAverage() {
		if (count < size)
			return 0;
		return sum / count;
	}
}
