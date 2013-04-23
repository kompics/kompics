/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics;

import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: Auto-generated Javadoc
/**
 * The <code>SpinlockQueue</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class SpinlockQueue<E> {

	private final ConcurrentLinkedQueue<E> q = new ConcurrentLinkedQueue<E>();
	
	public void offer(E e) {
		q.offer(e);
	}
	
	public E poll() {
		return q.poll();
	}
	
	public E peek() {
		return q.peek();
	}
	
	public boolean isEmpty() {
		return q.isEmpty();
	}
        
        public void remove(E e) {
            q.remove(e);
        }

//	public static final int FL_SIZE = 1000;
//
//	private final Spinlock lock = new Spinlock();
//
//	static final ThreadLocal<FreeList> freeList = new ThreadLocal<FreeList>() {
//		@Override
//		protected FreeList initialValue() {
//			return new FreeList(null, 0);
//		};
//	};
//
//	static class FreeList {
//		Node<?> head;
//		int size;
//		int foundEmpty;
//		int foundFull;
////		int allocated;
////		int freed;
//
//		public FreeList(Node<?> next, int size) {
//			this.head = next;
//			this.size = size;
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	private Node<E> allocate(E value, Node<E> next) {
//		FreeList free = freeList.get();
//		Node<E> node = (Node<E>) free.head;
////		free.allocated++;
//		if (node == null) { // nothing to recycle
//			free.foundEmpty++;
//			return new Node<E>(value, next);
//		}
//		// recycle existing node
//		free.head = node.next;
//		free.size--;
//		// initialize
//		node.item = value;
//		node.next = next;
//		return node;
//	}
//
//	@SuppressWarnings("unchecked")
//	private void free(Node<E> node) {
//		FreeList free = freeList.get();
////		free.freed++;
//		if (free.size < FL_SIZE) {
//			node.next = (Node<E>) free.head;
//			node.item = null;
//			free.head = node;
//			free.size++;
//			return;
//		}
//		free.foundFull++;
//	}
//
//	// @SuppressWarnings("unchecked")
//	// private void free(Node<E> node) {
//	// Head<E> free = freeList.get();
//	// node.next = free.next;
//	// node.item = null;
//	// free.next = node;
//	// free.size++;
//	//
//	// if (free.size >= SIZE) {
//	// free.next = null;
//	// free.size = 0;
//	// }
//	// }
//
//	static class Node<E> {
//		E item;
//		Node<E> next;
//
//		public Node(E item, Node<E> next) {
//			this.item = item;
//			this.next = next;
//		}
//	}
//
//	private Node<E> head;
//	private Node<E> tail;
//
//	/**
//	 * Instantiates a new spinlock queue.
//	 */
//	public SpinlockQueue() {
//		head = new Node<E>(null, null);
//		tail = head;
//	}
//
//	/**
//	 * Offer.
//	 * 
//	 * @param e
//	 *            the e
//	 */
//	public void offer(E e) {
//		if (e == null)
//			throw new NullPointerException();
//		Node<E> n = allocate(e, null);
//		lock.lock();
//		try {
//			tail.next = n;
//			tail = n;
//		} finally {
//			lock.unlock();
//		}
//	}
//
//	/**
//	 * Poll.
//	 * 
//	 * @return the e
//	 */
//	public E poll() {
//		E e = null;
//		Node<E> removed = null;
//		lock.lock();
//		try {
//			if (head == tail)
//				return null;
//			removed = head;
//			head = head.next;
//			e = head.item;
//		} finally {
//			lock.unlock();
//		}
//		free(removed);
//		return e;
//	}
//
//	/**
//	 * Gets the stats.
//	 * 
//	 * @return the stats
//	 */
//	public static int[] getStats() {
//		FreeList free = freeList.get();
//		int ret[] = new int[4];
//		ret[0] = free.foundEmpty;
//		ret[1] = free.foundFull;
////		ret[2] = free.allocated;
////		ret[3] = free.freed;
//		return ret;
//	}
//
//	/**
//	 * Reset stats.
//	 */
//	public static void resetStats() {
//		FreeList free = freeList.get();
//		free.foundEmpty = 0;
//		free.foundFull = 0;
////		free.allocated = 0;
////		free.freed = 0;
//	}
}
