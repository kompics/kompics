/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.master.swing.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.master.swing.exp.ExpEntry.ExperimentStatus;
import se.sics.kompics.wan.ssh.Host;

/**
 *
 * @author jdowling
 */
public class HostTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -291524554830459027L;

    private static final Logger logger = LoggerFactory.getLogger(HostTableModel.class);


    private static String[] columnNames = {"Hostname"
//        ,"SessionId"
//        ,"Status"
    };
    private List<Host> availableHosts = new ArrayList<Host>();
    private Map<Host, ExperimentStatus> expStatus = new HashMap<Host, ExperimentStatus>();

    public HostTableModel() {
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {

        return availableHosts.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {

        Host h = availableHosts.get(row);
        if (col == 0) {
            return h.getHostname();
        } else if (col == 1) {
            return h.getSessionId();
        } else if (col == 2) {
            return expStatus.get(h);
        } else {
            throw new IndexOutOfBoundsException("Column number; " + col);
        }
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (col < 2) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {

        Host h = availableHosts.get(row);

        if (col == 0) {
            h.setHostname((String) value);
        } else if (col == 1) {
            h.setSessionId((Integer) value);
        } else if (col == 2) {
            expStatus.put(h, (ExperimentStatus) value);
        } else {
            throw new IndexOutOfBoundsException("Column number; " + col);
        }
        fireTableCellUpdated(row, col);
    }

    public void addHosts(Set<Host> hosts) {
        for (Host h : hosts) {
            addHost(h);
        }
    }

    private void addHost(Host host) {
        boolean update = false;
        if (availableHosts.contains(host) == true) {
            update = true;
        }
        availableHosts.add(host);
        expStatus.put(host, ExperimentStatus.NOT_LOADED);
        int index = availableHosts.indexOf(host);
        if (update == false) {
            fireTableRowsInserted(index, index);
        } else {
            fireTableRowsUpdated(index, index);
        }
    }

    public void removeHosts(Set<Host> hosts) {
        for (Host h : hosts) {
            int index = availableHosts.indexOf(h);
            if (index != -1) {
                availableHosts.remove(h);
                expStatus.remove(h);
                fireTableRowsDeleted(index, index + 1);
            }
            else {
                logger.warn("Index out-of-bounds when trying to remove host");
            }
        }
    }

    public void replaceHosts(Set<Host> hosts) {
        int size = availableHosts.size();
        availableHosts.clear();
        if (size > 0) {
            fireTableRowsDeleted(0, size);
        }
        addHosts(hosts);
    }
}


