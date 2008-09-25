package se.sics.kompics.core.scheduler;

public class DoubleFreelistSpinlockQueue<E> {

	public static int SIZE = 1000;

	private Spinlock lock = new Spinlock();

	@SuppressWarnings("unchecked")
	private static final ThreadLocal<FreeList> freeList = new ThreadLocal<FreeList>() {
		protected FreeList initialValue() {
			return new FreeList(null, 0);
		};
	};

	static class FreeList<E> {
		Node<E> head;
		Node<E> tail;
		int size;

		public FreeList(Node<E> next, int size) {
			this.head = next;
			this.tail = head;
			this.size = size;
		}
	}

	@SuppressWarnings("unchecked")
	private Node<E> allocate(E value, Node<E> next) {
		FreeList<E> free = freeList.get();
		Node<E> node = free.head;
		if (node == null) { // nothing to recycle
			return new Node<E>(value, next);
		}
		// recycle existing node
		free.head = node.next;
		if (free.tail == node) {
			free.tail = null;
		}
		free.size--;
		// initialize
		node.item = value;
		node.next = next;
		return node;
	}

	@SuppressWarnings("unchecked")
	private void free(Node<E> node) {
		FreeList<E> free = freeList.get();
		if (free.size < SIZE) {
			node.next = free.head;
			node.item = null;
			node.prev = null;
			if (free.head != null) {
				free.head.prev = node;
			}
			free.head = node;
			free.size++;
			if (free.tail == null) {
				free.tail = node;
			}
		} else {
			node.next = free.head;
			node.item = null;
			node.prev = null;
			if (free.head != null) {
				free.head.prev = node;
			}
			free.head = node;
			// we assume tail is not null, i.e. SIZE > 0
			// discard tail
			Node<E> last = free.tail;
			free.tail = free.tail.prev;
//			last.next = null;
			last.prev = null;
		}
	}

	// @SuppressWarnings("unchecked")
	// private void free(Node<E> node) {
	// Head<E> free = freeList.get();
	// node.next = free.next;
	// node.item = null;
	// free.next = node;
	// free.size++;
	//
	// if (free.size >= SIZE) {
	// free.next = null;
	// free.size = 0;
	// }
	// }

	static class Node<E> {
		E item;
		Node<E> next;
		Node<E> prev;

		public Node(E item, Node<E> next) {
			this.item = item;
			this.next = next;
		}
	}

	private Node<E> head;
	private Node<E> tail;

	public DoubleFreelistSpinlockQueue() {
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
