/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx;

import java.awt.Component;
import java.awt.Container;
import java.util.ResourceBundle;

import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/**
 * Various utility methods.
 *
 * @author sky
 */
public final class Utilities {
    Utilities() {
    }
    
    public static void registerDefaults(String path) {
        ResourceBundle bundle = ResourceBundle.getBundle(path);
        for (String key : bundle.keySet()) {
            UIManager.put(key, bundle.getObject(key));
        }
    }

    /**
     * Makes the specified component recursively visible. If the specified
     * component is contained in a tab pane, the tab is selected.
     *
     * @param c the component to make recursively visible
     * @throws NullPointerException if c is null.
     */
    public static void makeVisible(Component c) {
        if (!c.isShowing()) {
            Component parent = c;
            while (parent != null) {
                Container nextParent = parent.getParent();
                // Note, this should handle internal frames as well
                if (nextParent instanceof JTabbedPane) {
                    JTabbedPane tp = (JTabbedPane)nextParent;
                    int index = tp.indexOfComponent(parent);
                    if (index != -1 && tp.getSelectedIndex() != index) {
                        tp.setSelectedIndex(index);
                    }
                }
                parent = nextParent;
            }
        }
    }
}
