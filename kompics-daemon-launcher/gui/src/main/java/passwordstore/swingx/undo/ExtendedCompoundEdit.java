/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.undo;

import javax.swing.undo.CompoundEdit;

/**
 * A CompoundEdit that maintains a reference to an ExtendedUndoManager. When
 * end is invoked, the undo managers actions are updated appropriately.
 *
 * @author sky
 */
public class ExtendedCompoundEdit extends CompoundEdit {
    private ExtendedUndoManager undoManager;
    
    public ExtendedCompoundEdit(ExtendedUndoManager undoManager) {
        this.undoManager = undoManager;
    }

    public void end() {
        if (isInProgress()) {
            super.end();
            undoManager.updateActions();
            undoManager = null;
        }
    }
}
