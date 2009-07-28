/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.ui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

/**
 * Abstracts away the ability to save files. This is needed to distinguish
 * between running from the command line, and running from web start.
 *
 * @version $Revision$
 */
abstract class PersistenceHandler {
    private static final boolean inWebStart;
    
    static {
        boolean inWS;
        try {
            ServiceManager.lookup("javax.jnlp.BasicService");
            inWS = true;
        } catch (UnavailableServiceException ex) {
            inWS = false;
        }
        inWebStart = inWS;
    }
    
    static PersistenceHandler getHandler() {
        if (inWebStart) {
            return new JWSPersistenceHandler();
        }
        return new FilePersistenceHandler();
    }
    
    PersistenceHandler() {
    }
    
    public abstract boolean exists();
    
    public abstract InputStream getInputStream() throws IOException;
    
    public abstract OutputStream getOutputStream() throws IOException;
    

    private static class FilePersistenceHandler extends PersistenceHandler {
        // Returns the path to save the password file.
        // For demo purposes this is saved in the users home directory. In a
        // real app the user would be prompted for the location.
        private String getContentsPath() {
            return System.getProperty("user.home") + File.separator +
                    ".passwordStoreEntries.xml";
        }
        
        public boolean exists() {
            return new File(getContentsPath()).exists();
        }
        
        public OutputStream getOutputStream() throws IOException {
            return new BufferedOutputStream(
                    new FileOutputStream(getContentsPath()));
        }
        
        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(
                                               getContentsPath()));
        }
    }
}
