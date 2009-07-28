/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.awt.datatransfer.DataFlavor;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import passwordstore.swingx.CutCopyPasteHelper;

/**
 * UIFactory is used as a way to detect when a new Component has been created.
 * UIFactory is only used for JTextComponents to register the necessary listener
 * to update the enabled state of the cut, copy and paste actions.
 *
 * @author sky
 */
public class UIFactory {
    private static final DataFlavor[] PLAIN_TEXT_FLAVORS;

    static {
	try {
	    PLAIN_TEXT_FLAVORS = new DataFlavor[5];
	    PLAIN_TEXT_FLAVORS[0] = new DataFlavor("text/plain;class=java.lang.String");
	    PLAIN_TEXT_FLAVORS[1] = new DataFlavor("text/plain;class=java.io.Reader");
	    PLAIN_TEXT_FLAVORS[2] = new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream");
            PLAIN_TEXT_FLAVORS[3] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.String");
	    PLAIN_TEXT_FLAVORS[4] = DataFlavor.stringFlavor;
	} catch (ClassNotFoundException cle) {
            throw new RuntimeException("Error creating TextTransferable", cle);
	}
    }
    
    public static ComponentUI createUI(JComponent c) {
        ComponentUI ui = UIManager.getLookAndFeelDefaults().getUI(c);
        if (c instanceof JTextComponent) {
            if (((JTextComponent)c).getUI() == null) {
                // If the UI is null, it means we're in the constructor and
                // haven't installed anything yet.
                CutCopyPasteHelper.registerCutCopyPasteBindings(c, false);
                CutCopyPasteHelper.registerDataFlavors(c, PLAIN_TEXT_FLAVORS);
                ((JTextComponent)c).addCaretListener(new CaretHandler());
            }
        }
        return ui;
    }
    
    
    private static final class CaretHandler implements CaretListener {
        public void caretUpdate(CaretEvent e) {
            JTextComponent text = (JTextComponent) e.getSource();
            Caret caret = text.getCaret();
            boolean selection = (caret.getDot() != caret.getMark());
            boolean editable = text.isEditable();
            CutCopyPasteHelper.setCopyEnabled(text, selection);
            CutCopyPasteHelper.setCutEnabled(text, editable && selection);
            CutCopyPasteHelper.setPasteEnabled(text, editable);
        }
    }
}
