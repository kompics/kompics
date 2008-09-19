package se.sics.kompics.core.scheduler;

public class FreelistSpinlockQueue<E> {

	public static final int SIZE = 0;

	private Spinlock lock = new Spinlock();

	@SuppressWarnings("unchecked")
	private static final ThreadLocal<Head> freeList = new ThreadLocal<Head>() {
		protected Head initialValue() {
			return new Head(null, 0);
		};
	};

	static class Head<E> {
		Node<E> next;
		int size;

		public Head(Node<E> next, int size) {
			this.next = next;
			this.size = size;
		}
	}

	@SuppressWarnings("unchecked")
	private Node<E> allocate(E value, Node<E> next) {
		Head<E> free = freeList.get();
		Node<E> node = free.next;
		if (node == null) { // nothing to recycle
			return new Node<E>(value, next);
		}
		// recycle existing node
		free.next = node.next;
		free.size--;
		// initialize
		node.item = value;
		node.next = next;
		return node;
	}

	@SuppressWarnings("unchecked")
	private void free(Node<E> node) {
		Head<E> free = freeList.get();
		node.next = free.next;
//		node.item = null;
//		free.next = node;
//		free.size++;
//
//		if (free.size >= SIZE) {
//			free.next = null;
//			free.size = 0;
//		}
	}

	static class Node<E> {
		E item;
		Node<E> next;

		public Node(E item, Node<E> next) {
			this.item = item;
			this.next = next;
		}
	}

	private Node<E> head;
	private Node<E> tail;

	public FreelistSpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
	}

	public void offer(E e) {
		if (e == null)
			throw new NullPointerException();
		Node<E> n = allocate(e, null);
		lock.lock();
		try {
			tail.next = n;
			tail = n;
		} finally {
			lock.unlock();
		}
	}

	// public E poll() {
	// E e = null;
	// lock.lock();
	// try {
	// if (head == tail)
	// return null;
	// head.item=null;
	// head = head.next;
	// e = head.item;
	// } finally {
	// lock.unlock();
	// }
	// return e;
	// }

	public E poll() {
		E e = null;
		Node<E> removed = null;
		lock.lock();
		try {
			if (head == tail)
				return null;
			removed = head;
			head = head.next;
			e = head.item;
		} finally {
			lock.unlock();
		}
		free(removed);
		return e;
	}
}
