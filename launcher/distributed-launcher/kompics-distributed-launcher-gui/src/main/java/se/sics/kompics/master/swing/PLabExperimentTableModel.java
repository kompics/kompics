/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.master.swing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import se.sics.kompics.wan.ssh.Host;

/**
 *
 * @author jdowling
 */
   class PLabExperimentTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -291524554830459027L;

       private static String[] columnNames = {"Hostname",
                                        "Site",
                                        "SessionId",
                                        "Status"};

       	private final static int NUM_COLUMNS;

        static {
            NUM_COLUMNS = PLabExperimentTableModel.columnNames.length;
        }


        private List<Host> availableHosts = new ArrayList<Host>();

        public static enum Status {NOT_CONNECTED,CONNECTED,UNAVAILABLE};


        private Object[][] data = {
            {"evgsics1", "SICS",
             new Integer(5), Status.CONNECTED}
        };

        public PLabExperimentTableModel()
        {
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {

            return availableHosts.size();
//            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {

            Host h = availableHosts.get(row);
            if (col == 0) {
                return h.getHostname();
            }
            else if (col == 1) {
                return "SICS";
            }
            else if (col == 2) {
               return h.getSessionId();
            }
            else if (col == 3) {
                return "status";
            }
            else {
                 throw new IndexOutOfBoundsException("Column number; " + col);
            }
//            return data[row][col];
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
//            data[row][col] = value;

            Host h = availableHosts.get(row);
            if (col == 0) {
                h.setHostname((String) value);
            }
            else if (col == 1) {
                // TODO
            }
            else if (col == 2) {
               h.setSessionId((Integer) value);
            }
            else if (col == 3) {
                // TODO
            }
            else {
                 throw new IndexOutOfBoundsException("Column number; " + col);
            }
            fireTableCellUpdated(row, col);
        }

        public void addHosts(Set<Host> hosts) {
            int size = availableHosts.size();
//            availableHosts.addAll(hosts);
//            int newSize = availableHosts.size();
            for (Host h : hosts) {
            	addHost(h);
            }
//            fireUpdated(size, newSize);
        }
        
        private void addHost(Host host) {
        	boolean update = false;
        	if (availableHosts.contains(host) == true) {
        		update = true;
        	}
        	availableHosts.add(host);
        	int index = availableHosts.indexOf(host);
        	if (update == false) {
        		fireTableRowsInserted(index, index);
        	}
        	else {
        		fireTableRowsUpdated(index, index);
        	}
        }

        public void removeHosts(Set<Host> hosts)
        {
        	int oldSize = availableHosts.size();
        	for (Host h : hosts) {
        		int index = availableHosts.indexOf(h); 
        		if (index != -1) {
        			availableHosts.remove(h);
            		fireTableRowsDeleted(index, index+1);
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


