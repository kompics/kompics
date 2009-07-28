/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.swingx.app;

import java.util.EventListener;

/**
 * ApplicationListener is used by Application to notify listeners of major
 * events during the lifecycle of an Application.
 *
 * @version $Revision$
 */
public interface ApplicationListener extends EventListener {
    /**
     * Invoked after the application has finished intializing.
     */
    public void applicationDidInit();
    
    /**
     * Invoked to determine if the application should exit. A return value
     * of false will stop the application from exiting.
     *
     * @return true if the application should be allowed to exit, otherwise
     *         false
     */
    public boolean canApplicationExit();
    
    /**
     * Notification that the application is about to exit.
     */
    public void applicationExiting();
}
