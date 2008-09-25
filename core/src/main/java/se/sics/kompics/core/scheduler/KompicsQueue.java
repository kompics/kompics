package se.sics.kompics.core.scheduler;

public class KompicsQueue<E> {

	// public ConcurrentLinkedQueue<E> queue;
	public FreelistSpinlockQueue<E> queue;

	// public DoubleFreelistSpinlockQueue<E> queue;
	// private IDFreelistSpinlockQueue<E> queue;
	// private BadFreelistSpinlockQueue<E> queue;
	// public SpinlockQueue<E> queue;

	public KompicsQueue() {
		// queue = new ConcurrentLinkedQueue<E>();
		// queue = new IDFreelistSpinlockQueue<E>();
		queue = new FreelistSpinlockQueue<E>();
		// queue = new DoubleFreelistSpinlockQueue<E>();
		// queue = new BadFreelistSpinlockQueue<E>();
		// queue = new SpinlockQueue<E>();
	}

	public void offer(E e) {
		queue.offer(e);
	}

	public E poll() {
		return queue.poll();
	}
}
