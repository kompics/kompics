package se.sics.kompics.core.scheduler;

public class SpinlockQueue<E> {

	private Spinlock lock = new Spinlock();

	@SuppressWarnings("unchecked")
	static ThreadLocal<Node> freeList = new ThreadLocal<Node>() {
		protected Node initialValue() {
			return null;
		};
	};

	@SuppressWarnings("unchecked")
	private Node<E> allocate(E value) {
		Node<E> node = freeList.get();
		if (node == null) { // nothing to recycle
			node = new Node<E>(value, null);
		} else { // recycle existing node
			freeList.set(node.next);
		}
		// initialize
		node.item = value;
		return node;
	}

	@SuppressWarnings("unchecked")
	private void free(Node<E> node) {
		Node<E> free = freeList.get();
		node.next = free;
		freeList.set(node);
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

	public SpinlockQueue() {
		head = new Node<E>(null, null);
		tail = head;
	}

	public void offer(E e) {
		if (e == null)
			throw new NullPointerException();
		lock.lock();
		try {
			Node<E> n = allocate(e);
			// Node n = new Node(e, null);
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
