package se.sics.kompics.core.sched;

public class FairPriorityReadyComponent implements Runnable {

	private FairPriorityReadyQueue fairPriorityReadyQueue;

	public FairPriorityReadyComponent(
			FairPriorityReadyQueue fairPriorityReadyQueue) {
		super();
		this.fairPriorityReadyQueue = fairPriorityReadyQueue;
	}

	public void run() {
		ReadyComponent readyComponent = fairPriorityReadyQueue.take();
		readyComponent.run();
	}
}
