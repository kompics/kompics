/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.beansx;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An abstract class providing management of PropertyChangeListeners.
 * Non-visual beans should extend this to get PropertyChangeListener management
 * for free.
 *
 * @author sky
 */
public abstract class BeanBase {
    private final PropertyChangeSupport support;
    
    public BeanBase() {
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
