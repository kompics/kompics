/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.undo;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import passwordstore.swingx.app.Application;

/**
 * An UndoableEdit that changes a property of a bean.
 *
 * @author sky
 */
public class PropertyUndoableEdit extends AbstractUndoableEdit {
    private final Object bean;
    private final String property;
    private Object lastValue;
    
    public PropertyUndoableEdit(Object bean, String property, Object lastValue) {
        if (bean == null || property == null) {
            throw new IllegalArgumentException(
                    "Source and property must be non-null");
        }
        this.bean = bean;
        this.property = property;
        this.lastValue = lastValue;
    }
    
    public void redo() throws CannotRedoException {
        super.redo();
        swapValues();
    }
    
    public void undo() throws CannotUndoException {
        super.undo();
        swapValues();
    }

    private void swapValues() {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(
                    property, bean.getClass());
            Object currentValue = pd.getReadMethod().invoke(bean);
            pd.getWriteMethod().invoke(bean, lastValue);
            lastValue = currentValue;
        } catch (IllegalArgumentException ex) {
            Application.getInstance().uncaughtException(ex);
        } catch (InvocationTargetException ex) {
            Application.getInstance().uncaughtException(ex);
        } catch (IllegalAccessException ex) {
            Application.getInstance().uncaughtException(ex);
        } catch (IntrospectionException ex) {
            Application.getInstance().uncaughtException(ex);
        }
    }
    
    public String toString() {
        return getClass().getName() + " [bean=" + bean +
                ", property=" + property +
                ", lastValue=" + lastValue + "]";
    }
}
