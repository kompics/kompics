package se.sics.kompics.core.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import se.sics.kompics.api.Priority;

public class Scheduler {

	private FairPriorityReadyQueue fairPriorityReadyQueue;

	private BlockingQueue<Runnable> readyQueue;

	private ThreadPoolExecutor threadPoolExecutor;

	private boolean fair;

	public Scheduler(int workers, int fairnessRate) {
		super();

		this.fair = (fairnessRate > 0);

		if (fair) {
			fairPriorityReadyQueue = new FairPriorityReadyQueue(fairnessRate);
			readyQueue = new LinkedBlockingQueue<Runnable>();
		} else {
			readyQueue = new PriorityBlockingQueue<Runnable>();
		}

		threadPoolExecutor = new ThreadPoolExecutor(workers, workers, 0L,
				TimeUnit.SECONDS, readyQueue);
	}

	public boolean isFair() {
		return fair;
	}

	public void componentReady(ReadyComponent readyComponent) {
		if (fair) {
			fairPriorityReadyQueue.put(readyComponent);
			FairPriorityReadyComponent fprc = new FairPriorityReadyComponent(
					fairPriorityReadyQueue);

			threadPoolExecutor.execute(fprc);
		} else {
			threadPoolExecutor.execute(readyComponent);
		}
	}

	public void publishedEvent(Priority priority) {
		if (fair) {
			fairPriorityReadyQueue.publishedEvent(priority);
		}
	}

	public void executedEvent(Priority priority) {
		if (fair) {
			fairPriorityReadyQueue.executedEvent(priority);
		}
	}
}
