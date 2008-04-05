package se.sics.kompics.core.sched;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import se.sics.kompics.api.Priority;
import se.sics.kompics.core.ComponentCore;

public class Scheduler {

	private FairPriorityReadyQueue fairPriorityReadyQueue;

	private BlockingQueue<Runnable> readyQueue;

	private ThreadPoolExecutor threadPoolExecutor;

	private boolean fair;

	public Scheduler(int workers, boolean fair) {
		super();

		this.fair = fair;

		if (fair) {
			fairPriorityReadyQueue = new FairPriorityReadyQueue();
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

	public void componentReady(ComponentCore componentCore, Priority priority) {
		ReadyComponent readyComponent = new ReadyComponent(componentCore,
				priority);

		if (fair) {
			fairPriorityReadyQueue.put(readyComponent);
			FairPriorityReadyComponent fprc = new FairPriorityReadyComponent(
					fairPriorityReadyQueue);

			threadPoolExecutor.execute(fprc);
		} else {
			threadPoolExecutor.execute(readyComponent);
		}
	}

	public void executedEvent(Priority priority) {
		// TODO fairness
	}

	public void publishedEvent(Priority priority) {
		// TODO fairness
	}
}
