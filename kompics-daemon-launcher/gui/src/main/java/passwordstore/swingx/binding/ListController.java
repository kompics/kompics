/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import passwordstore.beansx.BeanBase;
import passwordstore.collectionsx.CollectionsX;
import passwordstore.collectionsx.ObservableList;
import passwordstore.collectionsx.ObservableListListener;

/**
 * ListController manages a List of entries, a selection of the entries,
 * and a filter. PropertyChangeListeners are notified as any of these
 * properties change.
 * <p>
 * This class is abstract, subclasses must override the includeEntry to
 * determine if a value should be included in the current filter string.
 *
 * @author sky
 */
public abstract class ListController<T> extends BeanBase {
    private String filter;
    private List<T> prefilteredEntries;
    private List<T> entries;
    private List<T> selection;

    public ListController() {
        selection = Collections.emptyList();
        selection = Collections.unmodifiableList(selection);
        entries = new ArrayList<T>(1);
        entries = CollectionsX.observableList(entries);
    }
    
    public final void setEntries(List<T> entries) {
        List<T> oldEntries = this.entries;
        this.entries = entries;
        // NOTE: this is needed do to List implementing a deep equals method
        firePropertyChange("entries", null, null);
    }
    
    public final List<T> getEntries() {
        return entries;
    }
    
    public final void setSelection(List<T> selection) {
        List<T> oldSelection = this.selection;
        if (selection == null) {
            selection = Collections.emptyList();
        }
        this.selection = new ArrayList<T>(selection);
        this.selection = Collections.unmodifiableList(this.selection);
        firePropertyChange("selection", oldSelection, null);
    }
    
    public final List<T> getSelection() {
        return selection;
    }
    
    public final void setSelectedEntry(T entry) {
        if (entry == null) {
            setSelection(null);
        } else {
            List<T> selection = new ArrayList<T>(1);
            selection.add(entry);
            setSelection(selection);
        }
    }
    
    public final T getSelectedEntry() {
        if (selection.size() > 0) {
            return selection.get(0);
        }
        return null;
    }
    
    public void setFilter(String filter) {
        String oldFilter = this.filter;
        this.filter = filter;
        filterChanged(filter);
        firePropertyChange("filter", oldFilter, filter);
    }
    
    public String getFilter() {
        return filter;
    }
    
    public boolean isFiltered() {
        return (prefilteredEntries != null);
    }
    
    private void filterChanged(String filter) {
        if (filter == null || filter.length() == 0) {
            if (prefilteredEntries != null) {
                setEntries(prefilteredEntries);
                prefilteredEntries = null;
            }
        } else {
            filter = filter.toLowerCase();
            List<T> entries;
            if (prefilteredEntries == null) {
                prefilteredEntries = this.entries;
                entries = this.entries;
            } else {
                entries = prefilteredEntries;
            }
            List<T> filteredEntries = new ArrayList<T>(entries.size());
            for (T entry : entries) {
                if (includeEntry(entry, filter)) {
                    filteredEntries.add(entry);
                }
            }
            ObservableList<T> oEntries = CollectionsX.observableList(
                    filteredEntries);
            filteredEntries = oEntries;
            oEntries.addObservableListListener(new FilteredListListener());
            setEntries(filteredEntries);
        }
        List<T> entries = getEntries();
        if (entries.size() > 0) {
            setSelectedEntry(entries.get(0));
        } else {
            setSelection(null);
        }
    }

    protected abstract boolean includeEntry(T entry, String filter);
    
    
    private class FilteredListListener implements ObservableListListener<T> {
        public void listElementsAdded(ObservableList<T> list, int index,
                int length) {
            for (int i = 0; i < length; i++) {
                prefilteredEntries.add(list.get(index + i));
            }
        }

        public void listElementsRemoved(ObservableList<T> list, int index,
                List<T> oldElements) {
            for (T elem : oldElements) {
                prefilteredEntries.remove(elem);
            }
        }

        public void listElementReplaced(ObservableList<T> list, int index,
                T oldElement) {
            prefilteredEntries.set(prefilteredEntries.indexOf(oldElement),
                    list.get(index));
        }

        public void listElementPropertyChanged(ObservableList list, int index) {
        }
    }
}
