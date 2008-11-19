package se.sics.kompics.core.stats;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.LinkedBlockingQueue;

public class NetStatsProcessor extends Thread {

	static {
		System.err.println("Proc v. 1-10");
	}

	private static final int PERIOD = 512 * 8;

	LinkedBlockingQueue<double[]> queue;

	double avg = 0;

	int count = 0;

	BufferedWriter out;
	
	MovingAverage ma1, ma10;

	public NetStatsProcessor(LinkedBlockingQueue<double[]> queue)
			throws FileNotFoundException {
		this.queue = queue;
		this.out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(System
						.getProperty("datafile", "plot.data"))));
		ma1 = new MovingAverage(PERIOD);
		ma10 = new MovingAverage(PERIOD * 100);
	}

	public void run() {
		while (true) {
			try {
				double data[] = queue.take();
				processData(data);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// MA, aggregation done in statserver
	void processData(double[] data) {
		count++;
		double mean = computeMean(data);

		ma1.pushData(data);
		double movingAvg1 = ma1.getMovingAverage();

		ma10.pushData(data);
		double movingAvg10 = ma10.getMovingAverage();

		avg = avg + (mean - avg) / count;

		try {
			out.write(count + " " + avg + " " + movingAvg1 + " " + movingAvg10
					+ "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println(count + " " + avg + " " + movingAvg1 + " "
				+ movingAvg10 + "\n");
	}

	// no MA with aggregation done in worker
	// void processData(double[] data) {
	// for (int i = 0; i < data.length; i++) {
	// processPoint(data[i]);
	// }
	// }
	//
	// void processPoint(double d) {
	// count++;
	//
	// avg = avg + (d - avg) / count;
	//
	// try {
	// out.write(count + " " + avg + " " + d + "\n");
	// out.flush();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// System.err.println(count + " " + avg + " " + d);
	// }

	// no MA with aggregation done in statserver
	// void processData(double[] data) {
	// count++;
	// double mean = computeMean(data);
	//
	// avg = avg + (mean - avg) / count;
	//
	// try {
	// out.write(count + " " + avg + " " + mean + "\n");
	// out.flush();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// System.err.println(count + " " + avg + " " + mean);
	// }

	private double computeMean(double[] data) {
		double sum = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
			
			if (data[i] < 0) throw new RuntimeException("Negative tau");
		}
		return sum / data.length;
	}
}
