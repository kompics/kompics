package se.sics.kompics.core.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import se.sics.kompics.core.ComponentCore;

public class Scheduler {

	private int workerCount;

	private BlockingQueue<Runnable> readyQueue;

	private ThreadPoolExecutor threadPoolExecutor;

	public Scheduler(int workers, int fairnessRate) {
		this.workerCount = workers;

		readyQueue = new LinkedBlockingQueue<Runnable>();

		threadPoolExecutor = new ThreadPoolExecutor(workers, workers, 0L,
				TimeUnit.SECONDS, readyQueue);
	}

	public void componentReady(ComponentCore readyComponent) {
		threadPoolExecutor.execute(readyComponent);
	}

	public int setWorkerCount(int workerCount) {
		if (workerCount > 0 && workerCount <= 32) {
			threadPoolExecutor.setCorePoolSize(workerCount);
			threadPoolExecutor.setMaximumPoolSize(workerCount);
			this.workerCount = workerCount;
		}
		return this.workerCount;
	}
}
