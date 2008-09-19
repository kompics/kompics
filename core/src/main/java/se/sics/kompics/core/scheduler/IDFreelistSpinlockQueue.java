package se.sics.kompics.core.scheduler;

public class IDFreelistSpinlockQueue<E> {

	public static final int SIZE = 0;

	private Spinlock lock = new Spinlock();

	private Head<E>[] freeListHead;
	
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
		if (id < 0) return new Node<E>(value, next);
		
		Head<E> free = freeListHead[id];
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

	private void free(Node<E> node) {
		int id = ThreadID.get();
		if (id < 0) return;

		Head<E> free = freeListHead[id];
		node.next = free.next;
		node.item = null;
		free.next = node;
		free.size++;

		if (free.size >= SIZE) {
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

	@SuppressWarnings("unchecked")
	public IDFreelistSpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
		int workers = 8;
		freeListHead = new Head[workers];
		for (int i = 0; i < workers; i++) {
			freeListHead[i] = new Head<E>(null, 0);
		}
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
