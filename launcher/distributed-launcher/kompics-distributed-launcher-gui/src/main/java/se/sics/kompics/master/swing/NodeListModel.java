/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.master.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.master.swing.model.NodeEntry;
import se.sics.kompics.wan.ssh.Host;

/**
 *
 * @author jdowling
 */
public class NodeListModel extends AbstractListModel implements PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeListModel.class);

    private LinkedHashSet<NodeEntry> nodes = new LinkedHashSet<NodeEntry>();

    public NodeListModel() {
    }

    public NodeListModel(Set<NodeEntry> nodes) {
        this.nodes = new LinkedHashSet<NodeEntry>(nodes);
    }

    public void setNodes(Set<NodeEntry> nodes) {
        for (NodeEntry h : nodes) {
            addNodeEntry(h);
        }
    }

    @Override
    public int getSize() {
        return nodes.size();
    }

    @Override
    public Object getElementAt(int arg0) {
        List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);

        return listNodes.get(arg0);
    }

    public NodeEntry indexOf(int index) {
        List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);
        return listNodes.get(index);
    }

//    public void add(int index, NodeEntry h) {
//
//        if (nodes.contains(h) == true) {
//            nodes.add(index, h);
//            fireContentsChanged(h, index, index);
//        } else {
//            nodes.add(index, h);
//            fireIntervalAdded(h, index, index);
//        }
//    }
    public void addNodeEntry(NodeEntry h) {

        if (nodes.contains(h) == true) {
            List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);
            int index = listNodes.indexOf(h);
            nodes.add(h);
            fireContentsChanged(h, index, index);
            logger.info("Entry changed at {} at index {}", h.getHostname(), index);
        } else {
            nodes.add(h);
            List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);
            int index = listNodes.indexOf(h);
            fireIntervalAdded(h, index, index);
            logger.info("Entry added at {} at index {}", h.getHostname(), index);
            h.addPropertyChangeListener(this);
        }
    }

    public void addHost(Host h) {
        NodeEntry ne = new NodeEntry(h);
        addNodeEntry(ne);
    }

    public void removeNode(NodeEntry h) {
        List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);
        int index = listNodes.indexOf(h);
        nodes.remove(h);
        fireIntervalRemoved(h, index, index);
//        int index = nodes.indexOf(h);
//        removeHost(index);
    }

//    public void removeHost(int index) {
//        NodeEntry h = nodes.get(index);
//        nodes.remove(index);
//        fireIntervalRemoved(h, index, index);
//    }
    public NodeEntry getNodeEntry(Host host) {
        for (NodeEntry n : nodes) {
            if (host.equals(n.getHost())==true) {
                return n;
            }
        }
        return null;
    }

    public List<NodeEntry> subList(int firstIndex, int lastIndex) {
        List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);

        return listNodes.subList(firstIndex, lastIndex);
    }

    public void propertyChange(PropertyChangeEvent e) {

        if (e.getSource() instanceof NodeEntry) {
            entryChanged((NodeEntry) e.getSource(), e.getPropertyName(),
                e.getOldValue());
        }
    }

    private void entryChanged(NodeEntry nodeEntry,
            String propertyChanged, Object lastValue) {
//        assert (selectedEntry == nodeEntry);
        boolean addEdit = false;
        // A value in the selected entry has changed, update the UI.


        List<NodeEntry> listNodes = new ArrayList<NodeEntry>(nodes);
        int index = listNodes.indexOf(nodeEntry);
        // XXX this doesn't seem to work - updates not reflected.
        fireContentsChanged(nodeEntry.getHost(), index, index);
        logger.info("Entry changed from {} at index {}", lastValue, index);


        if (propertyChanged == NodeEntry.HOSTNAME) {
//            hostTF.setText(nodeEntry.getHost());
        } else if (propertyChanged == NodeEntry.DAEMON_INSTALL_STATUS) {
//            userTF.setText(nodeEntry.getUser());
        } else if (propertyChanged == NodeEntry.DAEMON_RUNNING_STATUS) {
            logger.info("Daemon running status changed to {}", nodeEntry.getDaemonRunningStatus());
        } else if (propertyChanged == NodeEntry.JAVA_STATUS) {
        } else if (propertyChanged == NodeEntry.DAEMON_CONNECTION_STATUS) {
        }


//        else if (propertyChanged == "password") {
//            passwordTF.setText(nodeEntry.getPassword());
//            visualizer.setPassword(nodeEntry.getPassword());
//        }

    }

    public LinkedHashSet<NodeEntry> getNodes() {
        return nodes;
    }


}
