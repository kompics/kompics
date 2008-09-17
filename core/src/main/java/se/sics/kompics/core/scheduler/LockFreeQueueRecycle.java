/*
 * LockFreeQueueRecycle.java
 *
 * Created on March 10, 2006, 9:05 AM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */

package se.sics.kompics.core.scheduler;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Lock-free queue. Based on Michael and Scott
 * http://doi.acm.org/10.1145/248052.248106
 * 
 * Changed so that deq() returns null on empty, so that it looks just like the
 * ConcurrentLinkedQueue.
 * 
 * @author Maurice Herlihy
 * @author Cosmin Arad
 * 
 */
public class LockFreeQueueRecycle<T> {

	private AtomicStampedReference<Node> head;

	private AtomicStampedReference<Node> tail;

	ThreadLocal<Node> freeList = new ThreadLocal<Node>() {
		protected Node initialValue() {
			return null;
		};
	};

	/**
	 * Create a new object of this class.
	 */
	public LockFreeQueueRecycle() {
		Node sentinel = new Node();
		head = new AtomicStampedReference<Node>(sentinel, 0);
		tail = new AtomicStampedReference<Node>(sentinel, 0);
	}

	private Node allocate(T value) {
		int[] stamp = new int[1];
		Node node = freeList.get();
		if (node == null) { // nothing to recycle
			node = new Node();
		} else { // recycle existing node
			freeList.set(node.next.get(stamp));
		}
		// initialize
		node.value = value;
		return node;
	}

	private void free(Node node) {
		Node free = freeList.get();
		node.next = new AtomicStampedReference<Node>(free, 0);
		freeList.set(node);
	}

	/**
	 * Enqueue an item.
	 * 
	 * @param value
	 *            Item to enqueue.
	 */
	public void offer(T value) {
		if (value == null)
			throw new NullPointerException();
		// try to allocate new node from local pool
		Node node = allocate(value);
		int[] lastStamp = new int[1];
		int[] nextStamp = new int[1];
		int[] stamp = new int[1];
		while (true) { // keep trying
			Node last = tail.get(lastStamp); // read tail
			Node next = last.next.get(nextStamp); // read next
			// are they consistent?
			if (last == tail.get(stamp) && stamp[0] == lastStamp[0]) {
				if (next == null) { // was tail the last node?
					// try to link node to end of list
					if (last.next.compareAndSet(next, node, nextStamp[0],
							nextStamp[0] + 1)) {
						// enq done, try to advance tail
						tail.compareAndSet(last, node, lastStamp[0],
								lastStamp[0] + 1);
						return;
					} else {
						System.err.println("2");
					}
				} else { // tail was not the last node
					// try to swing tail to next node
					tail.compareAndSet(last, next, lastStamp[0],
							lastStamp[0] + 1);
					System.err.println("1");
					if (--times == 0) {
						System.exit(0);
					}
				}
			} else {
				System.err.println("0");
			}
		}
	}

	private int times = 100;

	/**
	 * Dequeue an item.
	 * 
	 * @throws queue.EmptyException
	 *             The queue is empty.
	 * @return Item at the head of the queue.
	 */
	public T poll() {
		int[] lastStamp = new int[1];
		int[] firstStamp = new int[1];
		int[] nextStamp = new int[1];
		int[] stamp = new int[1];
		while (true) {
			Node first = head.get(firstStamp);
			Node last = tail.get(lastStamp);
			Node next = first.next.get(nextStamp);
			// are they consistent?
			if (first == head.get(stamp) && stamp[0] == firstStamp[0]) {
				if (first == last) { // is queue empty or tail falling behind?
					if (next == null) { // is queue empty?
						// throw new EmptyException();
						return null;
					}
					// tail is behind, try to advance
					tail.compareAndSet(last, next, lastStamp[0],
							lastStamp[0] + 1);
				} else {
					T value = next.value; // read value before dequeuing
					if (head.compareAndSet(first, next, firstStamp[0],
							firstStamp[0] + 1)) {
						free(first);
						if (value == null) {
							System.err
									.println("LOCK-FREE QUEUE not empty returns NULL");
							System.err.flush();
							System.exit(0);
						}
						return value;
					}
				}
			}
		}
	}

	/**
	 * Items are kept in a list of nodes.
	 */
	public class Node {
		/**
		 * Item kept by this node.
		 */
		public T value;
		/**
		 * Next node in the queue.
		 */
		public AtomicStampedReference<Node> next;

		/**
		 * Create a new node.
		 */
		public Node() {
			this.next = new AtomicStampedReference<Node>(null, 0);
		}
	}
}
