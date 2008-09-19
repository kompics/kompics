package se.sics.kompics.core.scheduler;

public class BadFreelistSpinlockQueue<E> {

	private Spinlock lock = new Spinlock();

	@SuppressWarnings("unchecked")
	private static final ThreadLocal<Node> freeList = new ThreadLocal<Node>() {
		protected Node initialValue() {
			return new Node(null, null);
		};
	};

	@SuppressWarnings("unchecked")
	private Node<E> allocate(E value, Node<E> next) {
		Node<E> free = freeList.get();
		Node<E> node = free.next;
		if (node == null) { // nothing to recycle
			return new Node<E>(value, next);
		}
		// recycle existing node
//		free.next = node.next;
		free = node.next;
		// initialize
		node.item = value;
		node.next = next;
		return node;
	}

	@SuppressWarnings("unchecked")
	private void free(Node<E> node) {
		Node<E> free = freeList.get();
		node.next = free.next;
		node.item = null;
		free = node;
//		free.next = node;
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

	public BadFreelistSpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
	}

	public void offer(E e) {
		if (e == null)
			throw new NullPointerException();
		lock.lock();
		try {
			Node<E> n = allocate(e, null);
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
