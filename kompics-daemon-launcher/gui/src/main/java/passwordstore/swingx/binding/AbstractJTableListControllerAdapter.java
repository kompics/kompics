/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.binding;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/**
 * An AbstractListControllerAdapter that targets a JTable.
 * AbstractJTableListControllerAdapter adapts the ListController to a 
 * TableModel. Subclasses must override the various TableModel methods in
 * this class to determine the number of columns, column count, column
 * name...
 *
 * @author sky
 */
public abstract class AbstractJTableListControllerAdapter<T> extends
        AbstractListControllerAdapter<T> {
    private JTable table;
    private TableModelImpl tableModel;
    private ListSelectionListener listSelectionListener;
    
    public AbstractJTableListControllerAdapter(ListController<T> controller,
            JTable table) {
        super(controller);

        this.table = table;

        listSelectionListener = new ListSelectionHandler();
        tableModel = new TableModelImpl();
        table.setModel(tableModel);
        table.getSelectionModel().addListSelectionListener(
                listSelectionListener);
        
        // Update the selection
        listControllerSelectionChanged();
    }
    
    public void dispose() {
        super.dispose();
        table.getSelectionModel().removeListSelectionListener(
                listSelectionListener);
        table.setModel(new DefaultTableModel());
    }
    
    protected abstract void setValueAt(Object aValue, int rowIndex,
            int columnIndex);
    protected abstract int getColumnCount();
    protected abstract Object getValueAt(int rowIndex, int columnIndex);
    protected abstract Class<?> getColumnClass(int columnIndex);
    protected abstract String getColumnName(int column);

    // Invoked when the Controller's selection has changed, and we didn't
    // initiate it. Selection needs to be applied to JTable.
    protected void listControllerSelectionChanged() {
        // Set changingSelection to true. This will avoid us trying to
        // apply the selection back to the Controller
        changingSelection = true;

        // Map the selection to the JTable.
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        for (T entry : listController.getSelection()) {
            int index = entries.indexOf(entry);
            index = table.convertRowIndexToView(index);
            selectionModel.addSelectionInterval(index, index);
        }
        selectionModel.setValueIsAdjusting(false);
        
        // Selection done changing. Set changingSelection to false to indicate
        // any changes in the JTable (or Controller) need to be propagated.
        changingSelection = false;
    }

    // Invoked when the selection in the JTable changes and we didn't initiate
    // it. The selection needs to be applied to the Controller.
    @SuppressWarnings("unchecked")
    private void tableSelectionChanged() {
        // Set changingSelection to true. This will avoid us trying to
        // apply the selection back to the Controller
        changingSelection = true;

        // Map the selection to the JTable.
        int[] indices = table.getSelectedRows();
        List<T> selection = new ArrayList<T>(indices.length);
        for (int i = 0; i < indices.length; i++) {
            int index = table.convertRowIndexToModel(indices[i]);
            selection.add(entries.get(index));
        }
        listController.setSelection(selection);
        
        // Selection done changing. Set changingSelection to false to indicate
        // any changes in the JTable (or Controller) need to be propagated.
        changingSelection = false;
    }

    protected void entriesChanged() {
        tableModel.fireTableDataChanged();
    }

    protected void listElementsAdded(int index, int length) {
        RowSorter sorter = table.getRowSorter();
        tableModel.fireTableRowsInserted(index, index + length - 1);
    }

    protected void listElementsRemoved(int index, List elements) {
        tableModel.fireTableRowsDeleted(index, index + elements.size() - 1);
    }

    protected void listElementReplaced(int index, Object oldElement) {
        tableModel.fireTableRowsUpdated(index, index);
    }

    protected void listElementPropertyChanged(int index) {
        tableModel.fireTableRowsUpdated(index, index);
    }
    
    
    private class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!changingSelection && !e.getValueIsAdjusting()) {
                tableSelectionChanged();
            }
        }
    }
    
    
    private class TableModelImpl extends AbstractTableModel {
        public int getRowCount() {
            return entries.size();
        }
        
        public int getColumnCount() {
            return AbstractJTableListControllerAdapter.this.getColumnCount();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return AbstractJTableListControllerAdapter.this.getValueAt(
                    rowIndex, columnIndex);
        }

        public Class<?> getColumnClass(int columnIndex) {
            return AbstractJTableListControllerAdapter.this.getColumnClass(
                    columnIndex);
        }
        
        public String getColumnName(int columnIndex) {
            return AbstractJTableListControllerAdapter.this.getColumnName(
                    columnIndex);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            AbstractJTableListControllerAdapter.this.setValueAt(
                    aValue, rowIndex, columnIndex);
        }
    }
}
