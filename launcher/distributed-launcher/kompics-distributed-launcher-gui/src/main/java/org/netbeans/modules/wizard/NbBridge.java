/*  The contents of this file are subject to the terms of the Common Development
and Distribution License (the License). You may not use this file except in
compliance with the License.
    You can obtain a copy of the License at http://www.netbeans.org/cddl.html
or http://www.netbeans.org/cddl.txt.
    When distributing Covered Code, include this CDDL Header Notice in each file
and include the License file at http://www.netbeans.org/cddl.txt.
If applicable, add the following below the CDDL Header, with the fields
enclosed by brackets [] replaced by your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]" */

package org.netbeans.modules.wizard;

import java.lang.reflect.Method;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.netbeans.api.wizard.WizardDisplayer;

/**
 * Non API class for accessing a few things in NetBeans via reflection.
 *
 * @author Tim Boudreau
 */
public final class NbBridge {
    private NbBridge() {}
    static Boolean inNetBeans = null;
    public static boolean inNetBeans() {
        if (inNetBeans == null) {
            try {
                Class clazz = Class.forName("org.openide.util.Lookup"); //NOI18N
                clazz = Class.forName("org.openide.util.NbBundle"); //NOI18N
                inNetBeans = Boolean.TRUE;
            } catch (Exception e) {
                inNetBeans = Boolean.FALSE;
            }
        }
        return inNetBeans.booleanValue();
    }

    private static Method lkpMethod;
    public static Method defLkpMethod;
    public static WizardDisplayer getFactoryViaLookup() {
        // Sknutson: make compatible with JDK 1.4.2
        if (inNetBeans()) {
            try {
                if (lkpMethod == null) {
                    Class clazz = Class.forName("org.openide.util.Lookup"); //NOI18N
                    defLkpMethod = clazz.getMethod("getDefault", null); //NOI18N
                    lkpMethod = clazz.getMethod("lookup", new Class[] { Class.class}); //NOI18N
                }
                Object o = defLkpMethod.invoke(null, new Object[0]);
                return (WizardDisplayer)
                        lkpMethod.invoke(o, (Object[]) 
                            new Class[] { WizardDisplayer.class});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Method bundleMethod;
    private static String getStringViaNbBundle(Class clazz, String key) {
        
        // ensure jdk 1.4.2 compatible
        if (inNetBeans()) {
            try {
                if (bundleMethod == null) {
                    Class c = Class.forName("org.openide.util.NbBundle"); //NOI18N
                    bundleMethod = c.getMethod("getMessage", new Class[] {Class.class, String.class}); //NOI18N
                }
                return (String) bundleMethod.invoke (null, new Object[] {
                    clazz, key
                });
            } catch (MissingResourceException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getString (String path, Class callerType, String key) {
        String result = getStringViaNbBundle (callerType, key);
        if (result == null) {
            result = getStringViaResourceBundle (path, key);
        }
        return result;
    }

    private static String getStringViaResourceBundle (String path, String key) {
        return ResourceBundle.getBundle(path).getString(key);
    }
}
