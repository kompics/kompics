/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.collectionsx;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Convenience methods for creating an ObservableList implementation wrapping
 * an existing List.
 *
 * @author sky
 */
public final class CollectionsX {
    /**
     * Creates an ObservableList implementation wrapping the specified List.
     *
     * @param list the underlying List
     * @return an ObservableList implementation
     */
    public static <E> ObservableList<E> observableList(List<E> list) {
        return new ObservableListImpl<E>(list, false);
    }

    /**
     * Creates an ObservableList implementation wrapping the specified List.
     * Use this when you can determine en element of the List has changed.
     *
     * @param list the underlying List
     * @param an ObservableListHelper wrapping the specified List
     */
    public static <E> ObservableListHelper<E> observableListHelper(List<E> list) {
        ObservableListImpl<E> oList = new ObservableListImpl<E>(list, true);
        return new ObservableListHelper<E>(oList);
    }
    

    /**
     * Helper classed used to send notification that an element in the List
     * has changed.
     *
     * @see #observableListHelper
     */
    public final static class ObservableListHelper<E> {
        private final ObservableListImpl<E> list;

        ObservableListHelper(ObservableListImpl<E> list) {
            this.list = list;
        }

        /**
         * Returns the ObservableList implementation.
         *
         * @return the ObservableList implementation.
         */
        public ObservableList<E> getObservableList() {
            return list;
        }

        /**
         * Sends notification that the specified element has changed.
         *
         * @param index the index of the element that has changed
         */
        public void fireElementChanged(int index) {
            list.fireElementChanged(index);
        }
    }

    private static final class ObservableListImpl<E> extends AbstractList<E>
            implements ObservableList<E> {
        private final boolean supportsElementPropertyChanged;
        private List<E> list;
        private List<ObservableListListener> listeners;
        
        ObservableListImpl(List<E> list, boolean supportsElementPropertyChanged) {
            this.list = list;
            listeners = new CopyOnWriteArrayList<ObservableListListener>();
            this.supportsElementPropertyChanged = supportsElementPropertyChanged;
        }

        public E get(int index) {
            return list.get(index);
        }

        public int size() {
            return list.size();
        }

        public E set(int index, E element) {
            E oldValue = list.set(index, element);
            for (ObservableListListener listener : listeners) {
                listener.listElementReplaced(this, index, oldValue);
            }
            return oldValue;
        }

        public void add(int index, E element) {
            list.add(index, element);
            modCount++;
            for (ObservableListListener listener : listeners) {
                listener.listElementsAdded(this, index, 1);
            }
        }

        public E remove(int index) {
            E oldValue = list.remove(index);
            modCount++;
            for (ObservableListListener listener : listeners) {
                listener.listElementsRemoved(this, index,
                        java.util.Collections.singletonList(oldValue));
            }
            return oldValue;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(size(), c);
        }
        
        public boolean addAll(int index, Collection<? extends E> c) {
            if (list.addAll(index, c)) {
                modCount++;
                for (ObservableListListener listener : listeners) {
                    listener.listElementsAdded(this, index, c.size());
                }
            }
            return false;
        }

        public void clear() {
            List<E> dup = new ArrayList<E>(list);
            list.clear();
            modCount++;
            for (ObservableListListener listener : listeners) {
                listener.listElementsRemoved(this, 0, dup);
            }
        }

        public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
        }

        public <T> T[] toArray(T[] a) {
            return list.toArray(a);
        }

        public Object[] toArray() {
            return list.toArray();
        }

        private void fireElementChanged(int index) {
            for (ObservableListListener listener : listeners) {
                listener.listElementPropertyChanged(this, index);
            }
        }

        public void addObservableListListener(ObservableListListener listener) {
            listeners.add(listener);
        }

        public void removeObservableListListener(ObservableListListener listener) {
            listeners.remove(listener);
        }

        public boolean supportsElementPropertyChanged() {
            return supportsElementPropertyChanged;
        }
    }
}
