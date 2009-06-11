package se.sics.kompics.wan.master.plab.plc.comon;

public class CoMonStat {

	private String[] data;

	private final static int RESPTIME = 3;

	private final static int LOAD_AVERAGE_5 = 18;

	private final static int CPU_SPEED = 13;

	private final static int MEM_TOTAL = 25;

	private final static int MEM_FREE = 27;

	public CoMonStat() {

	}

	public CoMonStat(String line) {
		this.data = line.split(",");
		if (this.data.length != CoMonManager.COMON_NUM_FIELDS) {

			System.err.println("comon returned strange row, length="
					+ this.data.length);
		}

	}

	public String[] getData() {
		return data;
	}

	public void setData(String[] data) {
		this.data = data;
	}

	public String getHostname() {
		return data[0];
	}

	public double getResponseTime() {
		String s = this.getField(RESPTIME);
		double d = this.parseDouble(s);

		// looks like comon marks querytime as 0 if failing
		if (d == 0) {
			return Double.NaN;
		}
		return d;
	}

	public double getLoadAverage() {
		String s = this.getField(LOAD_AVERAGE_5);
		return this.parseDouble(s);
	}

	public double getMemFree() {
		String s = this.getField(MEM_FREE);
		return this.parseDouble(s);
	}

	public double getMemTotal() {
		String s = this.getField(MEM_TOTAL);
		return this.parseDouble(s);
	}

	private String getField(int field) {
		if (field < data.length && field > 0) {
			return data[field];
		}

		return "";
	}

	private double parseDouble(String string) {

		try {
			if (string != null) {
				double resptime = Double.parseDouble(string);
				return resptime;
			}
		} catch (NumberFormatException e) {
			// ignore, there was an error
			// System.err.println("comon: " + this.hostname
			// + " parse error double: '" + string + "'");
		}
		return Double.NaN;
	}
}
