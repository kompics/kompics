/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/**
 * Helper class providing cut, copy and paste actions that target the selected
 * components's TransferHandler. The action's enabled state is updated to
 * reflect the ability for the focused component to perform cut, copy and
 * paste.
 * <p>
 * Use the various setter methods to indicate whether a component should
 * support cut, copy or paste. It is expected these methods will be invoked
 * when the selection changes. For example, for a JTextField you would install
 * a CaretListener to track changes to the selection and update the 
 * cut, copy and paste state appropriately.
 *
 * @author sky
 */
public final class CutCopyPasteHelper {
    private static final Action CUT_INSTANCE;
    private static final Action COPY_INSTANCE;
    private static final Action PASTE_INSTANCE;
    
    // NOTE, we use strings here as JComponent's fire method fires the
    // property change on the string value of this.
    private static final Object CUT_CLIENT_PROPERTY = "__cut__";
    private static final Object COPY_CLIENT_PROPERTY = "__copy__";
    private static final Object PASTE_CLIENT_PROPERTY = "__paste__";
  
    private static final Object FLAVORS_CLIENT_PROPERTY =
            new StringBuilder("PasteFlavorsClientProperty");

    private static final Clipboard CLIPBOARD;
    
    static {
        Clipboard clipboard;
        try {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (SecurityException e) {
            // Don't have access to the clipboard, create a new one
            clipboard = new Clipboard("Sandboxed Clipboard");
        }
        CLIPBOARD = clipboard;
        CUT_INSTANCE = new CutCopyAction(true);
        COPY_INSTANCE = new CutCopyAction(false);
        PASTE_INSTANCE = new PasteAction();
    }
    
    private static Clipboard getClipboard() {
        return CLIPBOARD;
    }

    /**
     * Returns an action to perform a cut operation.
     *
     * @return the cut action
     */
    public static Action getCutAction() {
        return CUT_INSTANCE;
    }
    
    /**
     * Returns an action to perform a copy operation.
     *
     * @return the copy action
     */
    public static Action getCopyAction() {
        return COPY_INSTANCE;
    }
    
    /**
     * Returns an action to perform a paste operation.
     *
     * @return the paste action
     */
    public static Action getPasteAction() {
        return PASTE_INSTANCE;
    }

    /**
     * Specifies whether the component can perform a cut operation.
     *
     * @param component the Component to set the enabled state for
     * @param enable true if component supports cut
     * @throws NullPointerException if component is null
     */
    public static void setCutEnabled(JComponent component, boolean enable) {
        component.putClientProperty(CUT_CLIENT_PROPERTY, enable);
    }
    
    /**
     * Returns whether the component can perform a cut operation.
     *
     * @param component the Component to set the enabled state for
     * @throws NullPointerException if component is null
     */
    private static boolean isCutEnabled(JComponent component) {
        return getBooleanClientProperty(component, CUT_CLIENT_PROPERTY);
    }
    
    /**
     * Specifies whether the component can perform a copy operation.
     *
     * @param component the Component to set the enabled state for
     * @param enable true if component supports copy
     * @throws NullPointerException if component is null
     */
    public static void setCopyEnabled(JComponent component, boolean enable) {
        component.putClientProperty(COPY_CLIENT_PROPERTY, enable);
    }
    
    /**
     * Returns whether the component can perform a copy operation.
     *
     * @param component the Component to set the enabled state for
     * @throws NullPointerException if component is null
     */
    private static boolean isCopyEnabled(JComponent component) {
        return getBooleanClientProperty(component, COPY_CLIENT_PROPERTY);
    }
    
    /**
     * Specifies whether the component can perform a paste operation.
     *
     * @param component the Component to set the enabled state for
     * @param enable true if component supports paste
     * @throws NullPointerException if component is null
     */
    public static void setPasteEnabled(JComponent component, boolean enable) {
        component.putClientProperty(PASTE_CLIENT_PROPERTY, enable);
    }

    /**
     * Returns whether the component can perform a paste operation.
     *
     * @param component the Component to set the enabled state for
     * @throws NullPointerException if component is null
     */
    private static boolean isPasteEnabled(JComponent component) {
        return getBooleanClientProperty(component, PASTE_CLIENT_PROPERTY);
    }
    
    private static boolean getBooleanClientProperty(JComponent c,
            Object property) {
        Boolean value = (Boolean)c.getClientProperty(property);
        return (value == null) ? false : value;
    }
    
    public static void registerDataFlavors(JComponent component,
            DataFlavor...dataFlavors) {
        component.putClientProperty(FLAVORS_CLIENT_PROPERTY,
                dataFlavors);
    }
    
    private static DataFlavor[] getDataFlavors(JComponent component) {
        return (DataFlavor[])component.getClientProperty(
                FLAVORS_CLIENT_PROPERTY);
    }
    
    /**
     * Resigers the appropriate key bindings for cut, copy, and paste on the
     * specified component. Registered bindings target the actions provided by
     * this class.
     *
     * @param component the component to register bindings for
     */
    public static void registerCutCopyPasteBindings(JComponent component) {
        registerCutCopyPasteBindings(component, false);
    }

    /**
     * Resigers the appropriate key bindings for cut, copy, and paste on the
     * specified component. Registered bindings target the actions provided by
     * this class.
     *
     * @param registerDelegate if true, KeyEvent.VK_DELETE is registered for
     *        cut
     * @param component the component to register bindings for
     */
    public static void registerCutCopyPasteBindings(JComponent component,
            boolean registerDelete) {
        InputMap inputMap = component.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("ctrl C"),
                COPY_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("ctrl X"),
                CUT_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("COPY"),
                COPY_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("CUT"),
                CUT_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("ctrl INSERT"),
                COPY_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("shift DELETE"),
                CUT_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("ctrl V"), PASTE_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("PASTE"), PASTE_INSTANCE);
        inputMap.put(KeyStroke.getKeyStroke("shift INSERT"), PASTE_INSTANCE);
        if (registerDelete){ 
            inputMap.put(KeyStroke.getKeyStroke("DELETE"), CUT_INSTANCE);
        }
        ActionMap actionMap = component.getActionMap();
        actionMap.put(CUT_INSTANCE, CUT_INSTANCE);
        actionMap.put(COPY_INSTANCE, COPY_INSTANCE);
        actionMap.put(PASTE_INSTANCE, PASTE_INSTANCE);
        actionMap.put(CUT_INSTANCE, CUT_INSTANCE);
    }

    private CutCopyPasteHelper() {
    }
    
    
    private static abstract class FocusedAction extends AbstractAction {
        private static List<WeakReference<FocusedAction>> focusedActions;
        
        private static void registerFocusedAction(FocusedAction action) {
            if (focusedActions == null) {
                focusedActions = new ArrayList<WeakReference<FocusedAction>>(1);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                        addPropertyChangeListener(new PropertyChangeHandler());
            }
            focusedActions.add(new WeakReference<FocusedAction>(action));
        }
        
        private static void updateActions(Component focusedComponent) {
            Iterator<WeakReference<FocusedAction>> actionIterator = 
                    focusedActions.iterator();
            while (actionIterator.hasNext()) {
                FocusedAction action = actionIterator.next().get();
                if (action == null) {
                    actionIterator.remove();
                } else {
                    action.update(focusedComponent);
                }
            }
        }
        
        FocusedAction() {
            registerFocusedAction(this);
        }
        
        protected void update() {
            update(KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    getPermanentFocusOwner());
        }

        protected abstract void update(Component permanentFocusOwner);

        
        private static final class PropertyChangeHandler implements
                PropertyChangeListener {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName() == "permanentFocusOwner") {
                    updateActions((Component)e.getNewValue());
                }
            }
        }
    }
    
    
    private static abstract class FocusedActionPCL extends FocusedAction
            implements PropertyChangeListener {
        private JComponent _focusedComponent;

        protected void update(Component permanentFocusOwner) {
            JComponent jFocusedComponent =
                    (permanentFocusOwner instanceof JComponent) ?
                        (JComponent)permanentFocusOwner : null;
            setFocusedComponent(jFocusedComponent);
        }
        
        protected void setFocusedComponent(JComponent component) {
            if (_focusedComponent != null) {
                _focusedComponent.removePropertyChangeListener(this);
            }
            _focusedComponent = component;
            if (_focusedComponent != null) {
                _focusedComponent.addPropertyChangeListener(this);
            } else {
                setEnabled(false);
            }
        }
        
        protected JComponent getFocusedComponent() {
            return _focusedComponent;
        }
    }
    

    private static final class CutCopyAction extends FocusedActionPCL {
        private boolean _isCut;
        
        CutCopyAction(boolean isCut) {
            _isCut = isCut;
            update();
        }
        
        public void actionPerformed(ActionEvent e) {
            int action = (_isCut) ? TransferHandler.MOVE : TransferHandler.COPY;
            Clipboard clipboard = getClipboard();
            JComponent component = getFocusedComponent();
            component.getTransferHandler().exportToClipboard(component, clipboard,
                    action);
        }

        protected void setFocusedComponent(JComponent component) {
            boolean validTarget = false;
            if (component != null) {
                TransferHandler handler = component.getTransferHandler();
                if (handler != null) {
                    int actions = component.getTransferHandler().
                            getSourceActions(component);
                    if ((_isCut && (actions & TransferHandler.MOVE) != 0) ||
                            (!_isCut && (actions & TransferHandler.COPY) != 0)) {
                        super.setFocusedComponent(component);
                        updateEnabledFromTarget();
                        validTarget = true;
                    }
                }
            }
            if (!validTarget) {
                super.setFocusedComponent(null);
            }
        }
        
        private void updateEnabledFromTarget() {
            setEnabled((_isCut && isCutEnabled(getFocusedComponent())) ||
                    (!_isCut && isCopyEnabled(getFocusedComponent())));
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            if ((_isCut && evt.getPropertyName() == CUT_CLIENT_PROPERTY) ||
                    (!_isCut && evt.getPropertyName() == COPY_CLIENT_PROPERTY)) {
                updateEnabledFromTarget();
            }
        }
    }


    private static final class PasteAction extends FocusedActionPCL {
        PasteAction() {
            getClipboard().addFlavorListener(new FlavorHandler());
            update();
        }
        
        protected void setFocusedComponent(JComponent component) {
            if (component != null && getDataFlavors(component) != null) {
                super.setFocusedComponent(component);
                updateEnabledFromTarget();
            } else {
                super.setFocusedComponent(null);
            }
        }
        
        private void updateEnabledFromTarget() {
            boolean enable = false;
            if (isPasteEnabled(getFocusedComponent())) {
                Clipboard clipboard = getClipboard();
                try {
                    for (DataFlavor flavor : getDataFlavors(
                            getFocusedComponent())) {
                        if (clipboard.isDataFlavorAvailable(flavor)) {
                            enable = true;
                            break;
                        }
                    }
                } catch (IllegalStateException ise) {
                    // Can't get at clipboard. Delay for a second and try again.
                    new Thread(new DelayedUpdateRunnable()).start();
                }
            }
            setEnabled(enable);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName() == PASTE_CLIENT_PROPERTY) {
                updateEnabledFromTarget();
            }
        }

        public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = getClipboard();
            JComponent target = getFocusedComponent();
            target.getTransferHandler().importData(
                    new TransferHandler.TransferSupport(target,
                    clipboard.getContents(null)));
        }
        
        
        private final class DelayedUpdateRunnable implements Runnable {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                SwingUtilities.invokeLater(new UpdateRunnable());
            }
        }
        
        
        private final class UpdateRunnable implements Runnable {
            public void run() {
                if (getFocusedComponent() != null) {
                    updateEnabledFromTarget();
                }
            }
        }


        private class FlavorHandler implements FlavorListener {
            public void flavorsChanged(FlavorEvent e) {
                if (getFocusedComponent() != null) {
                    updateEnabledFromTarget();
                }
            }
        }
    }
}
