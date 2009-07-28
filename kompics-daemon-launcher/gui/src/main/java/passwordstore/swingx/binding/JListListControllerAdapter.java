/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.binding;

import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * An AbstractListControllerAdapter that targets a JList.
 * JListListControllerAdapter adapts the ListController to a 
 * ListModel.
 *
 * @author sky
 */
public final class JListListControllerAdapter<T> extends
        AbstractListControllerAdapter<T> {
    private JList list;
    private ListModelImpl listModel;
    private ListSelectionListener listSelectionListener;
    
    public JListListControllerAdapter(ListController<T> controller, JList list) {
        super(controller);
        
        this.list = list;

        listSelectionListener = new ListSelectionHandler();
        listModel = new ListModelImpl();
        list.setModel(listModel);
        list.addListSelectionListener(listSelectionListener);
        
        // Update the selection
        listControllerSelectionChanged();
    }
    
    public void dispose() {
        super.dispose();
        list.removeListSelectionListener(listSelectionListener);
        list.setModel(new DefaultListModel());
    }

    // Invoked when the Controller's selection has changed, and we didn't
    // initiate it. Selection needs to be applied to JList.
    protected void listControllerSelectionChanged() {
        // Set changingSelection to true. This will avoid us trying to
        // apply the selection back to the Controller
        changingSelection = true;

        // Map the selection to the JList.
        ListSelectionModel selectionModel = list.getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        for (T entry : listController.getSelection()) {
            int index = entries.indexOf(entry);
            selectionModel.addSelectionInterval(index, index);
        }
        selectionModel.setValueIsAdjusting(false);
        
        // Selection done changing. Set changingSelection to false to indicate
        // any changes in the JList (or Controller) need to be propagated.
        changingSelection = false;
    }

    // Invoked when the selection in the JList changes and we didn't initiate
    // it. The selection needs to be applied to the Controller.
    @SuppressWarnings("unchecked")
    private void listSelectionChanged() {
        // Set changingSelection to true. This will avoid us trying to
        // apply the selection back to the Controller
        changingSelection = true;

        // Map the selection to the JList.
        List<T> selection = (List<T>)Arrays.asList(list.getSelectedValues());
        listController.setSelection(selection);
        
        // Selection done changing. Set changingSelection to false to indicate
        // any changes in the JList (or Controller) need to be propagated.
        changingSelection = false;
    }

    protected void entriesChanged() {
        listModel.entriesChanged();
    }

    protected void listElementsAdded(int index, int length) {
        listModel.fireIntervalAdded(index, index + length - 1);
    }

    protected void listElementsRemoved(int index, List elements) {
        listModel.fireIntervalRemoved(index, index + elements.size() - 1);
    }

    protected void listElementReplaced(int index, Object oldElement) {
        listModel.fireContentsChanged(index, index);
    }

    protected void listElementPropertyChanged(int index) {
        listModel.fireContentsChanged(index, index);
    }
    
    
    private class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!changingSelection && !e.getValueIsAdjusting()) {
                listSelectionChanged();
            }
        }
    }
    
    
    private class ListModelImpl extends AbstractListModel {
        public void entriesChanged() {
            fireContentsChanged(this, 0, entries.size());
        }
        
        public int getSize() {
            return entries.size();
        }

        public Object getElementAt(int index) {
            return entries.get(index);
        }

        protected void fireIntervalRemoved(int index0, int index1) {
            super.fireIntervalRemoved(this, index0, index1);
        }

        protected void fireIntervalAdded(int index0, int index1) {
            super.fireIntervalAdded(this, index0, index1);
        }

        protected void fireContentsChanged(int index0, int index1) {
            super.fireContentsChanged(this, index0, index1);
        }
    }
}
