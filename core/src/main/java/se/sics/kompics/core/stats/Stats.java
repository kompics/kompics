package se.sics.kompics.core.stats;

public class Stats {
	private int count;
	private double minTime;
	private double maxTime;
	private double m, newM;
	private double s, newS;

	public int getCount() {
		return count;
	}

	public double getMinTime() {
		return Math.max(0, minTime);
	}

	public double getMaxTime() {
		return maxTime;
	}

	public double getMeanTime() {
		return (count > 0) ? m : 0.0;
	}

	public double getVariance() {
		return ((count > 1) ? s / (count - 1) : 0.0);
	}

	public long getStdDevTime() {
		return (long) Math.sqrt(getVariance());
	}

	public String toString() {
		return "Count = " + count + "\nminTime = " + minTime + "\nmaxTime = "
				+ maxTime + "\nmeanTime = " + getMeanTime() + "\nvariance = "
				+ getVariance() + "\nstdDevTime = " + getStdDevTime();
	}

	public void push(double x) {
		count++;
		if (x < minTime || count == 1)
			minTime = x;
		if (x > maxTime)
			maxTime = x;

		newM = m + (x - m) / count;
		newS = s + (x - m) * (x - newM);

		m = newM;
		s = newS;
	}
}
