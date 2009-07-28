/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.collectionsx;

import java.util.List;

/**
 * A List implementation that notifies listeners of changes to the list.
 *
 * @author sky
 */
public interface ObservableList<E> extends List<E> {
    /**
     * Adds the specified listener to the list of listeners that are notified
     * of changse to the ObservableList.
     *
     * @param listener the ObservableListListener to add
     */
    public void addObservableListListener(ObservableListListener listener);

    /**
     * Removes the specified listener from the list of listeners that are notified
     * of changse to the ObservableList.
     *
     * @param listener the ObservableListListener to remove
     */
    public void removeObservableListListener(ObservableListListener listener);
    
    /**
     * Returns true if the ObservableList can notify listeners of changes to
     * a particular element. If this returns true, PropertyChangeListeners
     * need not be added to each element of the ObservableList.
     *
     * @return true if the ObservableList notifies listeners of changes to an
     *         element of the ObservableList
     */
    public boolean supportsElementPropertyChanged();
}
