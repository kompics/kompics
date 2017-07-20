/*
 * This file is part of the Kompics component model runtime.
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
package se.sics.kompics;

import java.util.HashMap;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
class HandlerStore {

    private HandlerEntry[] handlers = new HandlerEntry[0];
    private int totalSubscriptions = 0;
    private MatchedHandlerEntry[] matchers = new MatchedHandlerEntry[0];
    private int totalMatchers = 0;

    void subscribe(Handler h) {
        totalSubscriptions++;
        Class<? extends KompicsEvent> eventType = h.getEventType();
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].eventType.equals(eventType)) {
                handlers[i].add(h);
                return;
            }
        }
        HandlerEntry he = new HandlerEntry(eventType);
        he.add(h);
        HandlerEntry[] newHandlers = new HandlerEntry[handlers.length + 1];
        System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
        newHandlers[handlers.length] = he;
        handlers = newHandlers;
    }

    void subscribe(MatchedHandler h) {
        totalMatchers++;
        Class<? extends PatternExtractor> eventType = h.getCxtType();
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].eventType.equals(eventType)) {
                matchers[i].add(h);
                return;
            }
        }
        MatchedHandlerEntry mhe = new MatchedHandlerEntry(eventType);
        mhe.add(h);
        MatchedHandlerEntry[] newHandlers = new MatchedHandlerEntry[matchers.length + 1];
        System.arraycopy(matchers, 0, newHandlers, 0, matchers.length);
        newHandlers[matchers.length] = mhe;
        matchers = newHandlers;
    }

    boolean unsubscribe(Handler h) {
        Class<? extends KompicsEvent> eventType = h.getEventType();
        boolean found = false;
        int empties = 0;
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].eventType.equals(eventType)) {
                if (handlers[i].remove(h)) {
                    found = true;
                    totalSubscriptions--; // might be wrong if the same handler was subscribed more than once...but how would that make any sense?
                    if (handlers[i].isEmpty()) {
                        empties++;
                        handlers[i] = null;
                    }
                }
            }
        }
        if (empties > 0) {
            removeHandlers(empties);
        }
        return found;
    }

    boolean unsubscribe(MatchedHandler h) {
        Class<? extends PatternExtractor> eventType = h.getCxtType();
        boolean found = false;
        int empties = 0;
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].eventType.equals(eventType)) {
                if (matchers[i].remove(h)) {
                    found = true;
                    totalMatchers--;
                    if (matchers[i].isEmpty()) {
                        empties++;
                        matchers[i] = null;
                    }
                }
            }
        }
        if (empties > 0) {
            removeMatchers(empties);
        }
        return found;
    }

    private void removeHandlers(int empties) {
        if (empties >= handlers.length) {
            handlers = new HandlerEntry[0];
        } else {
            HandlerEntry[] newHandlers = new HandlerEntry[handlers.length - empties];
            int i = 0, j = 0;
            while (i < handlers.length) {
                if (handlers[i] != null) {
                    newHandlers[j] = handlers[i];
                    j++;
                }
                i++;
            }
            handlers = newHandlers;
        }
    }

    private void removeMatchers(int empties) {
        if (empties >= matchers.length) {
            matchers = new MatchedHandlerEntry[0];
        } else {
            MatchedHandlerEntry[] newHandlers = new MatchedHandlerEntry[matchers.length - empties];
            int i = 0, j = 0;
            while (i < matchers.length) {
                if (matchers[i] != null) {
                    newHandlers[j] = matchers[i];
                    j++;
                }
                i++;
            }
            matchers = newHandlers;
        }
    }

    boolean hasSubscription(KompicsEvent event) {
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].eventType.isInstance(event)) {
                return true;
            }
        }
        if (event instanceof PatternExtractor) {
            PatternExtractor pevent = (PatternExtractor) event;
            for (int i = 0; i < matchers.length; i++) {
                if (matchers[i].eventType.isInstance(pevent)) {
                    if (matchers[i].matches(pevent)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    HandlerList getSubscriptions(KompicsEvent event) {
        Handler[] subscriptions = new Handler[totalSubscriptions];
        int j = 0;
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].eventType.isInstance(event)) {
                for (int k = 0; k < handlers[i].subscriptions.length; k++) {
                    subscriptions[j] = handlers[i].subscriptions[k];
                    j++;
                }
            }
        }
        return new HandlerList(subscriptions, j);
    }

    MatchedHandlerList getMatchers(PatternExtractor event) {
        MatchedHandler[] subscriptions = new MatchedHandler[totalMatchers];
        int j = 0;
        for (int i = 0; i < matchers.length; i++) {
            if (matchers[i].eventType.isInstance(event)) {
                j = matchers[i].appendMatches(event, subscriptions, j);
            }
        }
        return new MatchedHandlerList(subscriptions, j);
    }

    private static class HandlerEntry {

        final Class<? extends KompicsEvent> eventType;
        private Handler[] subscriptions = new Handler[0];

        HandlerEntry(Class<? extends KompicsEvent> eventType) {
            this.eventType = eventType;
        }

        void add(Handler h) {
            Handler[] newSubscriptions = new Handler[subscriptions.length + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, subscriptions.length);
            newSubscriptions[subscriptions.length] = h;
            subscriptions = newSubscriptions;
        }

        boolean remove(Handler h) {
            Handler[] newSubscriptions = new Handler[subscriptions.length - 1];
            int i = 0, j = 0;
            boolean found = false;
            while (i < subscriptions.length) {
                if (subscriptions[i] == h) {
                    found = true;
                } else {
                    newSubscriptions[j] = subscriptions[i];
                    j++;
                }
                i++;
            }
            if (found) {
                subscriptions = newSubscriptions;
                return true;
            } else {
                return false;
            }
        }

        boolean isEmpty() {
            return subscriptions.length == 0;
        }

    }

    static class HandlerList {

        final Handler[] subscriptions;
        final int length;

        HandlerList(Handler[] subscriptions, int length) {
            this.subscriptions = subscriptions;
            this.length = length;
        }
    }

    private static class MatchedHandlerEntry {

        final Class<? extends PatternExtractor> eventType;
        private final HashMap<Object, MatchedHandler[]> subscriptions = new HashMap<>();

        MatchedHandlerEntry(Class<? extends PatternExtractor> ctxType) {
            this.eventType = ctxType;
        }

        void add(MatchedHandler h) {
            MatchedHandler[] handlers = subscriptions.get(h.pattern());
            if (handlers == null) {
                handlers = new MatchedHandler[]{h};
                subscriptions.put(h.pattern(), handlers);
            } else {
                MatchedHandler[] newHandlers = new MatchedHandler[handlers.length + 1];
                System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
                newHandlers[handlers.length] = h;
                subscriptions.put(h.pattern(), newHandlers);
            }
        }

        boolean remove(MatchedHandler h) {
            MatchedHandler[] handlers = subscriptions.get(h.pattern());
            if (handlers == null) {
                return false;
            } else {
                boolean found = false;
                if (handlers.length == 1) {
                    for (int i = 0; i < handlers.length; i++) {
                        if (handlers[i] == h) {
                            subscriptions.remove(h.pattern());
                            return true;
                        }
                    }
                } else {
                    MatchedHandler[] newHandlers = new MatchedHandler[handlers.length - 1];
                    int i = 0, j = 0;
                    while (i < handlers.length) {
                        if (handlers[i] == h) {
                            found = true;
                        } else {
                            newHandlers[j] = handlers[i];
                            j++;
                        }
                        i++;
                    }
                }
                return found;
            }
        }

        boolean isEmpty() {
            return subscriptions.isEmpty();
        }

        boolean matches(PatternExtractor pe) {
            Object p = pe.extractPattern();
            return subscriptions.containsKey(p);
        }

        int appendMatches(PatternExtractor pe, MatchedHandler[] matches, int jin) {
            Object p = pe.extractPattern();
            int j = jin;
            MatchedHandler[] handlers = subscriptions.get(p);
            if (handlers != null) {
                for (int k = 0; k < handlers.length; k++) {
                    matches[j] = handlers[k];
                    j++;
                }
            }
            return j;
        }
    }

    static class MatchedHandlerList {

        final MatchedHandler[] subscriptions;
        final int length;

        MatchedHandlerList(MatchedHandler[] subscriptions, int length) {
            this.subscriptions = subscriptions;
            this.length = length;
        }
    }
}
