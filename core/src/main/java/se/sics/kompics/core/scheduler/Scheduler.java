package se.sics.kompics.core.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import se.sics.kompics.core.ComponentCore;

public class Scheduler {

	private BlockingQueue<Runnable> readyQueue;

	private ThreadPoolExecutor threadPoolExecutor;

	public Scheduler(int workers, int fairnessRate) {
		readyQueue = new LinkedBlockingQueue<Runnable>();

		threadPoolExecutor = new ThreadPoolExecutor(workers, workers, 0L,
				TimeUnit.SECONDS, readyQueue);
	}

	public void componentReady(ComponentCore readyComponent) {
		threadPoolExecutor.execute(readyComponent);
	}
}
