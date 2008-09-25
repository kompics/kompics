package se.sics.kompics.core.scheduler;

public class SpinlockQueue<E> {

	private Spinlock lock = new Spinlock();

	public static class Node<E> {
		E item;
		Node<E> next;

		public Node(E item, Node<E> next) {
			this.item = item;
			this.next = next;
		}
	}

	private Node<E> head;
	private Node<E> tail;

	public SpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
	}

	public void offer(E e) {
		if (e == null)
			throw new NullPointerException();
		Node<E> n = new Node<E>(e, null);
		lock.lock();
		try {
			tail.next = n;
			tail = n;
		} finally {
			lock.unlock();
		}
	}

	public E poll() {
		E e = null;
		lock.lock();
		try {
			if (head != tail) {
				head = head.next;
				e = head.item;
			}
		} finally {
			lock.unlock();
		}
		return e;
	}
}
