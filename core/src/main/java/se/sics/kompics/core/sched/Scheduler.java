package se.sics.kompics.core.sched;

import java.util.concurrent.PriorityBlockingQueue;

public class Scheduler {

	private FairPriorityReadyQueue fairReadyQueue;

	private PriorityBlockingQueue<ReadyComponent> readyQueue;
}
