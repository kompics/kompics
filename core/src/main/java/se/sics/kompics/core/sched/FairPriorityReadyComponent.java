package se.sics.kompics.core.sched;

public class FairPriorityReadyComponent implements Runnable {

	private FairPriorityReadyQueue fairPriorityReadyQueue;

	public FairPriorityReadyComponent(
			FairPriorityReadyQueue fairPriorityReadyQueue) {
		super();
		this.fairPriorityReadyQueue = fairPriorityReadyQueue;
	}

	public void run() {
		// take a ready component from the fair priority queue
		ReadyComponent readyComponent = fairPriorityReadyQueue.take();
		readyComponent.run();
	}
}
