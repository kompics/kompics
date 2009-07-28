/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx;

import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Helper class for creating buttons and menus. If an & is present in the
 * text, it is mapped to the mnemonic.
 *
 * @author sky
 */
public final class MnemonicHelper {
    public static JMenu createMenu(String text) {
        JMenu menu = new JMenu();
        configureTextAndMnemonic(menu, text);
        return menu;
    }
    
    public static JMenuItem createMenuItem(JMenu parent, String text) {
        JMenuItem mi = new JMenuItem();
        configureTextAndMnemonic(mi, text);
        parent.add(mi);
        return mi;
    }
    
    public static JMenuItem createMenuItem(JMenu parent, String text,
            Action action) {
        JMenuItem mi = new JMenuItem(action);
        configureTextAndMnemonic(mi, text);
        parent.add(mi);
        return mi;
    }
    
    public static void configureTextAndMnemonic(AbstractButton button,
            String text) {
        int length = text.length();
        int index = text.indexOf('&');
        if (index == -1) {
            button.setText(text);
        } else {
            StringBuilder newText = new StringBuilder(length);
            int mnemonic = -1;
            int mnemonicIndex = -1;
            for (int i = 0; i < length; i++) {
                char aChar = text.charAt(i);
                if (aChar == '\\') {
                    if (i + 1 < length && text.charAt(i + 1) == '&') {
                        i++;
                        newText.append('&');
                    } else {
                        newText.append(aChar);
                    }
                } else if (aChar == '&') {
                    if (i + 1 < length) {
                        if (mnemonic != -1) {
                            throw new IllegalArgumentException(
                                    "Mnemonic already defined " + text);
                        }
                        aChar = text.charAt(i + 1);
                        if (aChar >= 'a' && aChar <= 'z') {
                            mnemonic = KeyEvent.VK_A + (aChar - 'a');
                        } else if (aChar >= 'A' && aChar <= 'Z') {
                            mnemonic = KeyEvent.VK_A + (aChar - 'A');
                        } else {
                            throw new IllegalArgumentException(
                                    "Not valid mnemonic " + text);
                        }
                        mnemonicIndex = newText.length();
                    } else {
                        newText.append(aChar);
                    }
                } else {
                    newText.append(aChar);
                }
            }
            button.setText(newText.toString());
            if (mnemonic != -1) {
                button.setMnemonic(mnemonic);
                button.setDisplayedMnemonicIndex(mnemonicIndex);
            }
        }
    }
    
    private MnemonicHelper() {
    }
}
