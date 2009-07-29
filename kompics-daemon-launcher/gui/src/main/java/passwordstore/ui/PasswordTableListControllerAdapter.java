/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.util.Date;

import javax.swing.JTable;

import passwordstore.model.HostEntry;
import passwordstore.model.NodeEntry;
import passwordstore.swingx.app.Application;
import passwordstore.swingx.binding.AbstractJTableListControllerAdapter;
import passwordstore.swingx.binding.ListController;

/**
 * AbstractJTableListControllerAdapter implementation for password store.
 *
 * @author sky
 */
final class PasswordTableListControllerAdapter extends AbstractJTableListControllerAdapter<NodeEntry> {
    PasswordTableListControllerAdapter(ListController<NodeEntry> controller,
            JTable table) {
        super(controller, table);
    }
    
    protected void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Don't allow editing of the JTable, so this isn't implemented.
    }
    
    protected int getColumnCount() {
        return 4;
    }
    
    protected Object getValueAt(int rowIndex, int columnIndex) {
    	NodeEntry entry = entries.get(rowIndex);
        switch(columnIndex) {
            case 0:
                return entries.get(rowIndex).getHost();
            case 1:
                return entries.get(rowIndex).getUser();
            case 2:
                return entries.get(rowIndex).getPassword();
            case 3:
                return new Date(entry.getLastModified());
        }
        assert false;
        return null;
    }
    
    protected Class<?> getColumnClass(int columnIndex) {
        switch(columnIndex) {
            case 0:
            case 1:
            case 2:
                return String.class;
            case 3:
                return Date.class;
        }
        assert false;
        return null;
    }
    
    protected String getColumnName(int column) {
        switch (column) {
            case 0:
                return Application.getResourceAsString("table.host");
            case 1:
                return Application.getResourceAsString("table.user");
            case 2:
                return Application.getResourceAsString("table.password");
            case 3:
                return Application.getResourceAsString("table.lastModified");
        }
        assert false;
        return null;
    }
}
