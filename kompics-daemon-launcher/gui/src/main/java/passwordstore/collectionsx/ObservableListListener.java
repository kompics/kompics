/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.collectionsx;

import java.util.EventListener;
import java.util.List;

/**
 * Listener type used for ObservableLists.
 *
 * @author sky
 */
public interface ObservableListListener<T> extends EventListener {
    /**
     * Notification that elements have been added to the list.
     *
     * @param list the ObservableList
     * @param index the starting index
     * @param length the number of elements added
     */
    public void listElementsAdded(ObservableList<T> list, int index, int length);

    /**
     * Notification that elements have been removed from the list.
     *
     * @param list the ObservableList
     * @param index the starting index
     * @param oldElements the elements that were removed
     */
    public void listElementsRemoved(ObservableList<T> list, int index, List<T> oldElements);

    /**
     * Notification than an element has been replaced
     *
     * @param list the ObservableList
     * @param index the index of the element that was replaced
     * @param the original element
     */
    public void listElementReplaced(ObservableList<T> list, int index, T oldElement);

    /**
     * Notification than a property of an element has changed. This will only
     * be sent if the ObservableList returns true from
     * supportsElementPropertyChanged.
     *
     * @param list the ObservableList
     * @param index the index of the element
     */
    public void listElementPropertyChanged(ObservableList<T> list, int index);
}
