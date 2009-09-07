package se.sics.kompics.master.swing.model;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import se.sics.kompics.wan.ssh.Host;

/**
 * The model for the PasswordStore applications. Consists of an ObservableList
 * with PasswordEntrys. The list can be archived using beans persistence.
 *
 * @version $Revision$
 */
public class NodeModel {
    private CopyOnWriteArrayList<NodeEntry> nodeEntries;
    
    public NodeModel(Set<Host> hosts) {
        nodeEntries = new CopyOnWriteArrayList<NodeEntry>();
        nodeEntries = (CopyOnWriteArrayList<NodeEntry>) CollectionsX.observableList(nodeEntries);

        for (Host h : hosts) {
            nodeEntries.add(new NodeEntry(h));
        }
    }
    
    public void load(String file) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(
                new FileInputStream(file));
        try {
            load(inputStream);
        } finally {
            inputStream.close();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void load(InputStream stream) {
        XMLDecoder decoder = null;
        decoder = new XMLDecoder(stream);
        decoder.setExceptionListener(new ExceptionHandler());
        List entries = (List)decoder.readObject();
        decoder.close();
        decoder = null;
        nodeEntries.clear();
        nodeEntries.addAll(entries);
    }
    
    public void save(String file) throws IOException {
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(file));
        try {
            save(outputStream);
        } finally {
            outputStream.close();
        }
    }
    
    public void save(OutputStream stream) throws IOException {
        XMLEncoder encoder = null;
        encoder = new XMLEncoder(stream);
        encoder.setExceptionListener(new ExceptionHandler());
        // XMLDecoder doesn't handle the private inner class that is created
        // by the call to ExtendedCollections.observableList. By
        // creating a new ArrayList we ensure it's saved correctly.
        encoder.writeObject(new ArrayList<NodeEntry>(nodeEntries));
        encoder.close();
        encoder = null;
    }
    
    public List<NodeEntry> getNodeEntries() {
        return nodeEntries;
    }

//    public void addNodeEntry(NodeEntry nodeEntry)
//    {
//        nodeEntries.add(nodeEntry);
//    }
   
    private static class ExceptionHandler implements ExceptionListener {
        public void exceptionThrown(Exception e) {
            if (e instanceof IOException) {
                throw new RuntimeException("IOException", e);
            }
        }
    }
}
