/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.binding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import passwordstore.collectionsx.ObservableList;
import passwordstore.collectionsx.ObservableListListener;

/**
 * AbstractListControllerAdapter provides the basis for classes displaying
 * the entries and selection from a ListController. AbstractListControllerAdapter
 * registers an ObservableListListener on the entries of the ListController and
 * invokes the various abstract methods of this class as the entries change.
 * 
 *
 * @author sky
 */
public abstract class AbstractListControllerAdapter<T> {
    private PropertyChangeListener propertyChangeListener;
    private ObservableListListener observableListListener;
    protected ListController<T> listController;
    protected List<T> entries;
    protected boolean changingSelection;
    
    public AbstractListControllerAdapter(ListController<T> listController) {
        this.listController = listController;
        propertyChangeListener = new PropertyChangeHandler();
        listController.addPropertyChangeListener(propertyChangeListener);

        // Install the necessary listeners
        updateEntries();
    }
    
    public void dispose() {
        listController.removePropertyChangeListener(propertyChangeListener);

        // Set this to ensure an NPE is generated if dispose is invoked twice.
        listController = null;
    }
    
    protected abstract void entriesChanged();
    protected abstract void listElementsAdded(int index, int length);
    protected abstract void listElementsRemoved(int index, List elements);
    protected abstract void listElementReplaced(int index, Object oldElement);
    protected abstract void listElementPropertyChanged(int index);
    protected abstract void listControllerSelectionChanged();
    
    private void handleListControllerSelectionChanged() {
        listControllerSelectionChanged();
    }
    
    private void handleListElementsAdded(int index, int length) {
        for (int i = 0; i < length; i++) {
            installPropertyChangeListener(entries.get(index + i));
        }
        listElementsAdded(index, length);
    }
    
    @SuppressWarnings("unchecked")
    private void handleListElementsRemoved(int index, List oldElements) {
        for (Object o : oldElements) {
            removePropertyChangeListener((T)o);
        }
        listElementsRemoved(index, oldElements);
    }
    
    @SuppressWarnings("unchecked")
    private void handleListElementReplaced(int index, Object oldElement) {
        removePropertyChangeListener((T)oldElement);
        installPropertyChangeListener(entries.get(index));
        listElementReplaced(index, oldElement);
    }
    
    private void handleListElementPropertyChanged(int index) {
        listElementPropertyChanged(index);
    }

    private void handleEntriesChanged() {
        updateEntries();
        entriesChanged();
    }
    
    private void updateEntries() {
        if (entries != null) {
            if (entries instanceof ObservableList) {
                ((ObservableList)entries).removeObservableListListener(
                        observableListListener);
            }
            for (T entry : entries) {
                removePropertyChangeListener(entry);
            }
        }
        entries = listController.getEntries();
        for (T entry : entries) {
            installPropertyChangeListener(entry);
        }
        if (entries instanceof ObservableList) {
            if (observableListListener == null) {
                observableListListener = new ObservableListHandler();
            }
            ((ObservableList)entries).addObservableListListener(
                    observableListListener);
        }
    }
    
    private void installPropertyChangeListener(T entry) {
        if (entry != null) {
            try {
                Class<?> klass = entry.getClass();
                Method pclMethod = klass.getMethod(
                        "addPropertyChangeListener", PropertyChangeListener.class);
                pclMethod.invoke(entry, propertyChangeListener);
            } catch (SecurityException ex) {
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            } catch (NoSuchMethodException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
    }
    
    private void removePropertyChangeListener(T entry) {
        if (entry != null) {
            try {
                Class<?> klass = entry.getClass();
                Method pclMethod = klass.getMethod(
                        "removePropertyChangeListener", PropertyChangeListener.class);
                pclMethod.invoke(entry, propertyChangeListener);
            } catch (SecurityException ex) {
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            } catch (NoSuchMethodException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
    }
    
    
    private class ObservableListHandler implements ObservableListListener {
        public void listElementsAdded(ObservableList list, int index,
                int length) {
            handleListElementsAdded(index, length);
        }

        public void listElementsRemoved(ObservableList list, int index,
                List oldElements) {
            handleListElementsRemoved(index, oldElements);
        }

        public void listElementReplaced(ObservableList list, int index,
                Object oldElement) {
            handleListElementReplaced(index, oldElement);
        }

        public void listElementPropertyChanged(ObservableList list, int index) {
            handleListElementPropertyChanged(index);
        }
    }
    
    
    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == listController) {
                String key = e.getPropertyName();
                if (key == "selection") {
                    if (!changingSelection) {
                        handleListControllerSelectionChanged();
                    }
                } else if (key == "entries") {
                    handleEntriesChanged();
                }
            } else {
                int index = entries.indexOf(e.getSource());
                if (index != -1) {
                    handleListElementPropertyChanged(index);
                }
            }
        }
    }
}
