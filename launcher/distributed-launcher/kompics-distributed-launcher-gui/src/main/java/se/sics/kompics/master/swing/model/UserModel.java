
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

public class UserModel {
    private UserEntry userEntry;
    
    public UserModel() {
        userEntry = new UserEntry();
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
        UserEntry entry = (UserEntry) decoder.readObject();
        decoder.close();
        decoder = null;
        userEntry = entry;
    }
    
    public void save(String file) throws IOException {
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(file, false));
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
        encoder.writeObject(userEntry);
        encoder.close();
        encoder = null;
    }
    
    public UserEntry getUserEntry() {
        return userEntry;
    }

    private static class ExceptionHandler implements ExceptionListener {
        public void exceptionThrown(Exception e) {
            if (e instanceof IOException) {
                throw new RuntimeException("IOException", e);
            }
        }
    }

    public void setUserEntry(UserEntry userEntry) {
        this.userEntry = userEntry;
    }
}
