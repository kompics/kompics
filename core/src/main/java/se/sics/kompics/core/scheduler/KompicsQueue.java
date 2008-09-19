package se.sics.kompics.core.scheduler;

public class KompicsQueue<E> {

//	private ConcurrentLinkedQueue<E> queue;
	private FreelistSpinlockQueue<E> queue;
//	private BadFreelistSpinlockQueue<E> queue;
//	private SpinlockQueue<E> queue;
	
	public KompicsQueue() {
//		queue = new ConcurrentLinkedQueue<E>();
		queue = new FreelistSpinlockQueue<E>();
//		queue = new BadFreelistSpinlockQueue<E>();
//		queue = new SpinlockQueue<E>();
	}
	
	public void offer(E e) {
		queue.offer(e);
	}
	
	public E poll() {
		return queue.poll();
	}
}
