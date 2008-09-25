package se.sics.kompics.core.scheduler;


@SuppressWarnings("unchecked")
public class IDFreelistSpinlockQueue<E> {

	public static final int SIZE = 1000;

	private Spinlock lock = new Spinlock();

	private static Head[] freeListHead;

	public static class Stat {
		public int foundEmpty;
		public int foundFull;
//		public int allocated;
//		public int freed;
	}

	public static Stat[] stat;
	
	public static int workers = 8;
	
	static {
		stat = new Stat[workers];
		freeListHead = new Head[workers];
		for (int i = 0; i < workers; i++) {
			freeListHead[i] = new Head(null, 0);
			stat[i] = new Stat();
		}
	}

	static class Head<E> {
		Node<E> next;
		int size;

		public Head(Node<E> next, int size) {
			this.next = next;
			this.size = size;
		}
	}

	private Node<E> allocate(E value, Node<E> next) {
		int id = ThreadID.get();
		if (id < 0) {
			return new Node<E>(value, next);
		}

		Head<E> free = freeListHead[id];
		Node<E> node = free.next;
//		stat[id].allocated++;
		if (node == null) { // nothing to recycle
//			stat[id].foundEmpty++;
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

	private void free(Node<E> node) {
		int id = ThreadID.get();
		if (id < 0) {
			return;
		}

		Head<E> free = freeListHead[id];
		node.next = free.next;
		node.item = null;
		free.next = node;
		free.size++;

//		stat[id].freed++;
		if (free.size >= SIZE) {
//			stat[id].foundFull++;
			free.next = null;
			free.size = 0;
		}
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

	public IDFreelistSpinlockQueue() {
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
