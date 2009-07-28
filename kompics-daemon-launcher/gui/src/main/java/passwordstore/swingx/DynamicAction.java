/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.swingx;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;

import passwordstore.swingx.app.Application;

/**
 * Implementation of Action that invokes a method on an Object.
 *
 * @version $Revision$
 */
public class DynamicAction extends AbstractAction {
    // Method to invoke
    private final String methodName;
    
    // Target for the method
    private final Object target;
    
    // Arguments to the method.
    private final Object[] args;

    /**
     * Creates a dynamic action with the specified arguments. When the
     * actionPerformed method is invoked on this class the method identified
     * by methodName is invoked with the specified arguments.
     *
     * @param target the Object to invoke the method on
     * @param methodName name of the method to invoke
     * @param args the arguments to pass to the method
     * @throws IllegalArgumentException if target or methodName is null
     */
    public DynamicAction(Object target, String methodName, Object...args) {
        if (target == null || methodName == null) {
            throw new IllegalArgumentException(
                    "Target and method name must be non-null");
        }
        this.target = target;
        this.methodName = methodName;
        this.args = args;
    }
    
    /**
     * Action method. When invoked this will invoke the named method on
     * the object passed to the constructor.
     *
     * @param e the ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        Class klass = target.getClass();
        try {
            Class[] argClasses = null;
            if (args != null && args.length > 0) {
                argClasses = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argClasses[i] = args[i].getClass();
                }
            }
            Method mid = klass.getMethod(methodName, argClasses);
            if (mid != null) {
                if (args != null) {
                    mid.invoke(target, args);
                } else {
                    mid.invoke(target);
                }
            }
        } catch (Exception exception) {
            Application.getInstance().uncaughtException(exception);
        }
    }
}
