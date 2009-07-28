/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import passwordstore.swingx.app.Application;

/**
 * A menu providing entries for the currently installed look and feels.
 *
 * @author sky
 */
public class LookAndFeelMenu extends JMenu {
    private boolean loadedLAFs;

    static {
        Utilities.registerDefaults("strings");
//        UIManager.getDefaults().addResourceBundle(
//                "passwordstore.swingx.resources.strings");
    }
    
    public LookAndFeelMenu() {
        super();
        MnemonicHelper.configureTextAndMnemonic(
                this, UIManager.getString("LookAndFeelMenu.title"));
        if (Application.isOSX()) {
            // On OS X when menus are promoted to the top of the screen, only
            // the contents get read. As such, there is no hook to determine
            // when the menu is first shown. So that this works at all, we
            // agressively load the look and feels.
            loadLookAndFeels();
        }
    }
    
    public void setPopupMenuVisible(boolean b) {
        if (b && !loadedLAFs) {
            // Lazily load the look and feels.
            loadLookAndFeels();
        }
        super.setPopupMenuVisible(b);
    }
    
    /**
     * Change the look and feel to the specified look and feel.
     *
     * @param lafClass name of the class to change the look and feel to
     */
    protected void changeLookAndFeel(String lafClass) {
        try {
            UIManager.setLookAndFeel(lafClass);
            updateUIs();
        } catch (ClassNotFoundException ex) {
            assert false;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            assert false;
        } catch (UnsupportedLookAndFeelException ex) {
            assert false;
        } catch (IllegalAccessException ex) {
            assert false;
        }
    }

    private void updateUIs() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            Dimension min = window.getMinimumSize();
            if (window.getWidth() < min.width ||
                    window.getHeight() < min.height) {
                window.setSize(Math.max(min.width, window.getWidth()),
                        Math.max(min.height, window.getHeight()));
            }
        }
    }

    private void loadLookAndFeels() {
        loadedLAFs = true;
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        LookAndFeel currentLAF = UIManager.getLookAndFeel();
        String currentLafClass = currentLAF.getClass().getName();
        ButtonGroup group = new ButtonGroup();
        ActionListener actionListener = new ChangeLookAndFeelActionHandler();
        for (UIManager.LookAndFeelInfo info : lafs) {
            LookAndFeel laf = null;
            if (info.getClassName().equals(currentLafClass)) {
                laf = currentLAF;
            } else if (!info.getClassName().equals("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel")) {
                try {
                    Class lafClass = Class.forName(info.getClassName());
                    laf = (LookAndFeel)lafClass.newInstance();
                    if (!laf.isSupportedLookAndFeel()) {
                        laf = null;
                    }
                } catch (ClassNotFoundException cnfe) {
                } catch (InstantiationException ie) {
                } catch (IllegalAccessException iae) {
                }
            }
            if (laf != null) {
                JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
                        info.getName());
                group.add(menuItem);
                if (laf == currentLAF) {
                    menuItem.setSelected(true);
                }
                menuItem.setActionCommand(info.getClassName());
                menuItem.addActionListener(actionListener);
                add(menuItem);
            }
        }
        try {
            Class.forName("com.incors.plaf.alloy.AlloyLookAndFeel");
            addAlloyMenu(group);
        } catch (ClassNotFoundException ex) {
        }
        try {
            Class.forName("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
            addSyntheticaMenu(group, actionListener);
        } catch (ClassNotFoundException ex) {
        }
    }

    private void addAlloyMenu(ButtonGroup group) {
        JMenu alloyMenu = new JMenu("Alloy");
        add(alloyMenu);
        addAlloyMenuItem(alloyMenu, group, "Default", null);
        addAlloyMenuItem(alloyMenu, group, "Bedouin", "com.incors.plaf.alloy.themes.bedouin.BedouinTheme");
        addAlloyMenuItem(alloyMenu, group, "Glass", "com.incors.plaf.alloy.themes.glass.GlassTheme");
        addAlloyMenuItem(alloyMenu, group, "Acid", "com.incors.plaf.alloy.themes.acid.AcidTheme");
    }

    private void addAlloyMenuItem(JMenu alloyMenu, ButtonGroup group,
            String name, final String themeClassName) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(name);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Class<?> alloyClass = Class.forName("com.incors.plaf.alloy.AlloyLookAndFeel");
                    LookAndFeel alloy = null;
                    if (themeClassName != null) {
                        for (Constructor<?> cons : alloyClass.getConstructors()) {
                            Class<?>[] args = cons.getParameterTypes();
                            if (args.length == 1 && args[0].getName().equals("com.incors.plaf.alloy.AlloyTheme")) {
                                alloy = (LookAndFeel) cons.newInstance(Class.forName(themeClassName).newInstance());
                            }
                        }
                    } else {
                        alloy = (LookAndFeel) alloyClass.newInstance();
                    }
                    UIManager.setLookAndFeel(alloy);
                    updateUIs();
                } catch (SecurityException ex) {
                } catch (IllegalArgumentException ex) {
                } catch (ClassNotFoundException ex) {
                } catch (InvocationTargetException ex) {
                } catch (UnsupportedLookAndFeelException ex) {
                } catch (InstantiationException ex) {
                } catch (IllegalAccessException ex) {
                }
            }
        });
        group.add(menuItem);
        alloyMenu.add(menuItem);
    }

    private void addSyntheticaMenu(ButtonGroup group, ActionListener actionListener) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem("Synthetica");
        group.add(menuItem);
        menuItem.setActionCommand("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
        menuItem.addActionListener(actionListener);
        add(menuItem);
    }


    private final class ChangeLookAndFeelActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            changeLookAndFeel(e.getActionCommand());
        }
    }
}
