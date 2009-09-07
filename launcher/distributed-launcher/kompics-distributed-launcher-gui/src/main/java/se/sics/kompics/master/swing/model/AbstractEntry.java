package se.sics.kompics.master.swing.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class AbstractEntry {
    private final PropertyChangeSupport support;
    
    public AbstractEntry() {
        support = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
    
    public void addPropertyChangeListener(String property,
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(property, listener);
    }
    
    public void removePropertyChangeListener(String property,
            PropertyChangeListener listener) {
        support.removePropertyChangeListener(property, listener);
    }
    
    protected void firePropertyChange(String key, Object oldValue,
            Object newValue) {
        support.firePropertyChange(key, oldValue, newValue);
    }
}
