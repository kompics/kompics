package se.sics.kompics.core.scheduler;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.core.ComponentCore;

public class Scheduler {

	private final static Logger logger = LoggerFactory
			.getLogger(Scheduler.class);

	static final long SLEEP_MS = 100;

	private int workerCount;

	private Worker[] workers;

	private Random random;

	public Scheduler(int workers, int fairnessRate) {
		this.workerCount = workers;
		this.random = new Random(0);
		this.workers = new Worker[workerCount];
		this.qsize = new int[workerCount];
		this.flsize = new int[workerCount];
		for (int i = 0; i < workerCount; i++) {
			this.workers[i] = new Worker(this, i);
		}
		for (int i = 0; i < workerCount; i++) {
			this.workers[i].start();
		}
	}

	public void componentReady(int wid, ComponentCore readyComponent) {
		if (wid < 0 || wid >= workerCount) {
			int w = random.nextInt(workerCount);
			workers[w].addWork(readyComponent);
			// synchronized (workers[w]) {
			// workers[w].notify();
			// }
		} else {
			workers[wid].addWork(readyComponent);
		}
	}

	int stealMoreWorkFromHighest(int wid) {
		ComponentCore core = null;
		int w = wid, maxsize = 0, qs, wm = (wid + 1) % workerCount, stolen = 0;

		for (int i = 0; i < workerCount - 1; i++) {
			w = (w + 1) % workerCount;
			qs = workers[w].qsize.get();
			if (maxsize < qs) {
				maxsize = qs;
				wm = w;
			}
		}

		int mqs = workers[wm].qsize.get();

		// mqs = (mqs > 3000 ? 3000 : mqs);

		if (mqs > 2) {
			for (int i = 0; i < mqs / 3; i++) {
				core = workers[wm].takeWork();
				if (core != null) {
					workers[wid].addWork(core);
					stolen++;
				}
			}
		}
		return stolen;
	}

	// not good: poor workers steal from other poor workers
	ComponentCore stealOneWorkFromNext(int wid) {
		ComponentCore core = null;
		int w = wid + 1;
		for (int i = 0; i < workerCount - 1; i++) {
			if (w == workerCount)
				w = 0;
			core = workers[w].takeWork();
			if (core != null) {
				return core;
			}
			w++;
		}
		return core;
	}

	ComponentCore stealOneWorkFromHighest(int wid) {
		int w = wid, maxsize = 0, qs, wm = (wid + 1) % workerCount;

		for (int i = 0; i < workerCount - 1; i++) {
			w = (w + 1) % workerCount;
			qs = workers[w].qsize.get();
			if (maxsize < qs) {
				maxsize = qs;
				wm = w;
			}
		}

		return workers[wm].takeWork();
	}

	/* === STATS === */
	private int reductions, steps;
	private int qsize[];
	private int flsize[];
	private double imbt, minimb = Double.MAX_VALUE, maximb, avgimb;
	private double flImbt, minFlImb = Double.MAX_VALUE, maxFlImb, avgFlImb;
	private double highDift, minHighDif = Double.MAX_VALUE, maxHighDif,
			avgHighDif;

	public void dumpStats() {
		for (int i = 0; i < workers.length; i++) {
			workers[i].resetWc();
			workers[i].wsStat((int) workers[i].avgws);
			logger
					.info("{} TWC={} TWS={} SWS={} FWS={} WC={},{},{} "
							+ "WS={},{},{} MQS={}", new Object[] {
							workers[i].getName(), workers[i].twc,
							workers[i].tws, workers[i].sws, workers[i].fws,
							workers[i].minwc, workers[i].maxwc,
							String.format("%.2f", workers[i].avgwc),
							workers[i].minws, workers[i].maxws,
							String.format("%.2f", workers[i].avgws),
							workers[i].maxQs });
		}
		logger.info("Total reductions: {}", reductions);
		logger.info("Work Queue size imbalance: Min={} Max={} Avg={} in {}"
				+ " steps.", new Object[] { String.format("%.2f", minimb),
				String.format("%.2f", maximb), String.format("%.2f", avgimb),
				steps });
		logger.info("FreeList size imbalance: Min={} Max={} Avg={} in {} "
				+ "steps.", new Object[] { String.format("%.2f", minFlImb),
				String.format("%.2f", maxFlImb),
				String.format("%.2f", avgFlImb), steps });
		logger.info("Work Queue size Maximum difference: Min={} Max={} Avg={}"
				+ " in {} steps.", new Object[] {
				String.format("%.2f", minHighDif),
				String.format("%.2f", maxHighDif),
				String.format("%.2f", avgHighDif), steps });
	}

	long getWorkStealingCount() {
		long cnt = 0;
		for (int i = 0; i < workers.length; i++) {
			cnt += workers[i].tws;
		}
		return cnt;
	}

	public void countReduction() {
		reductions++;
		if (reductions % 1000 == 0) {
			statStep();
		}
	}

	private void statStep() {
		int totalSize = 0, minSize = Integer.MAX_VALUE, maxSize = 0, totalFl = 0;
		for (int i = 0; i < workers.length; i++) {
			qsize[i] = workers[i].qsize.get();
			flsize[i] = workers[i].pool.size.get();
			totalSize += qsize[i];
			totalFl += flsize[i];
			if (minSize > qsize[i])
				minSize = qsize[i];
			if (maxSize < qsize[i])
				maxSize = qsize[i];
		}
		double avg = ((double) totalSize) / workerCount;
		double flAvg = ((double) totalFl) / workerCount;
		imbt = 0;
		flImbt = 0;
		highDift = maxSize - minSize;
		double a, f;
		for (int i = 0; i < workers.length; i++) {
			a = qsize[i] - avg;
			f = flsize[i] - flAvg;
			imbt += (a < 0 ? -a : a);
			flImbt += (f < 0 ? -f : f);
		}
		imbt /= workerCount;
		flImbt /= workerCount;

		steps++;

		if (minimb > imbt)
			minimb = imbt;
		if (maximb < imbt)
			maximb = imbt;
		avgimb = (avgimb * (steps - 1) + imbt) / (steps);

		if (minFlImb > flImbt)
			minFlImb = flImbt;
		if (maxFlImb < flImbt)
			maxFlImb = flImbt;
		avgFlImb = (avgFlImb * (steps - 1) + flImbt) / (steps);

		if (minHighDif > highDift)
			minHighDif = highDift;
		if (maxHighDif < highDift)
			maxHighDif = highDift;
		avgHighDif = (avgHighDif * (steps - 1) + highDift) / (steps);
	}

	public int setWorkerCount(int workerCount) {
		if (workerCount > 0 && workerCount <= 32) {
			// threadPoolExecutor.setCorePoolSize(workerCount);
			// threadPoolExecutor.setMaximumPoolSize(workerCount);
			// this.workerCount = workerCount;
		}
		return this.workerCount;
	}
}
