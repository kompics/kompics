/* 
 * This file is part of the CaracalDB distributed storage system.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.network.netty.serialization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class ReadLockTest {

    public static void main(String[] args) throws InterruptedException {        
        long executions = 10000000;
        int numT = 32;
        Random rand = new Random();
        String[] keys = new String[numT];
        Map<String, Integer> mymap = new HashMap<String, Integer>();
        for (int i = 0; i < numT; i++) {
            keys[i] = Integer.toString(rand.nextInt());
            mymap.put(keys[i], i);
        }
        List<Thread> threads = new LinkedList<Thread>();
        // NoLocks
        NoLocks service = new NoLocks(mymap);
        for (int i = 0; i < numT; i++) {
            Thread t = new Thread(new Worker(i, service, keys[i], executions));
            threads.add(t);
        }
        long startTS = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long stopTS = System.nanoTime();
        long time = stopTS - startTS;
        System.out.println("NoLocks finished in " + time + "ns with an average op time of " + (time / (numT * executions)) + "ns.");
        threads.clear();

        // ConcMap
        ConcMap service2 = new ConcMap(mymap);
        for (int i = 0; i < numT; i++) {
            Thread t = new Thread(new Worker(i, service2, keys[i], executions));
            threads.add(t);
        }
        startTS = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        stopTS = System.nanoTime();
        time = stopTS - startTS;
        System.out.println("ConcMap finished in " + time + "ns with an average op time of " + (time / (numT * executions)) + "ns.");
        threads.clear();

        // RWLock
        RWLock service3 = new RWLock(mymap);
        for (int i = 0; i < numT; i++) {
            Thread t = new Thread(new Worker(i, service3, keys[i], executions));
            threads.add(t);
        }
        startTS = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        stopTS = System.nanoTime();
        time = stopTS - startTS;
        System.out.println("RWLock finished in " + time + "ns with an average op time of " + (time / (numT * executions)) + "ns.");
        threads.clear();

        // SyncLock
        SyncLock service4 = new SyncLock(mymap);
        for (int i = 0; i < numT; i++) {
            Thread t = new Thread(new Worker(i, service4, keys[i], executions));
            threads.add(t);
        }
        startTS = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        stopTS = System.nanoTime();
        time = stopTS - startTS;
        System.out.println("SyncLock finished in " + time + "ns with an average op time of " + (time / (numT * executions)) + "ns.");
        threads.clear();
        
    }

    public static class Worker implements Runnable {

        private long counter = 0;
        private long sum = 0;
        private final LookupService service;
        private final String word;
        private final long executions;
        private final int id;

        public Worker(int id, LookupService service, String word, long executions) {
            this.service = service;
            this.word = word;
            this.executions = executions;
            this.id = id;
        }

        @Override
        public void run() {
            long startTS = System.nanoTime();
            while (counter < executions) {
                counter++;
                sum += service.lookup(word);
            }
            long endTS = System.nanoTime();
            long time = endTS - startTS;
            System.out.println("    Worker " + id + " finished in "
                    + time + "ns with average lookup time of "
                    + (time / executions) + "ns.");
        }

    }

    public static interface LookupService {

        public int lookup(String s);
    }

    public static class NoLocks implements LookupService {

        private final Map<String, Integer> m;

        public NoLocks(Map<String, Integer> m) {
            this.m = m;
        }

        public int lookup(String s) {
            return m.get(s);
        }
    }

    public static class ConcMap implements LookupService {

        private final ConcurrentMap<String, Integer> m;

        public ConcMap(Map<String, Integer> m) {
            this.m = new ConcurrentHashMap<String, Integer>(m);
        }

        public int lookup(String s) {
            return m.get(s);
        }
    }

    public static class RWLock implements LookupService {

        private final Map<String, Integer> m;
        private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

        public RWLock(Map<String, Integer> m) {
            this.m = m;
        }

        public int lookup(String s) {
            rwlock.readLock().lock();
            try {
                return m.get(s);
            } finally {
                rwlock.readLock().unlock();
            }
        }
    }

    public static class SyncLock implements LookupService {

        private final Map<String, Integer> m;

        public SyncLock(Map<String, Integer> m) {
            this.m = m;
        }

        public synchronized int lookup(String s) {
            return m.get(s);
        }
    }
}
