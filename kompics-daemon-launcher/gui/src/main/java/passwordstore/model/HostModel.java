/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.model;

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

import passwordstore.collectionsx.CollectionsX;

/**
 * The model for the PasswordStore applications. Consists of an ObservableList
 * with PasswordEntrys. The list can be archived using beans persistence.
 *
 * @version $Revision$
 */
public class HostModel {
    private List<HostEntry> hostEntries;
    
    public HostModel() {
        hostEntries = new ArrayList<HostEntry>(1);
        hostEntries = CollectionsX.observableList(hostEntries);
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
        hostEntries.clear();
        hostEntries.addAll(entries);
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
        encoder.writeObject(new ArrayList<HostEntry>(hostEntries));
        encoder.close();
        encoder = null;
    }
    
    public List<HostEntry> getHostEntries() {
        return hostEntries;
    }
    
    
    private static class ExceptionHandler implements ExceptionListener {
        public void exceptionThrown(Exception e) {
            if (e instanceof IOException) {
                throw new RuntimeException("IOException", e);
            }
        }
    }
}
