/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.undo;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * An UndoManager that ignores any edits while in the process of doing a
 * undo/redo. ExtendedUndoManager also provides actions whose enabled state
 * is updated appropriately to reflect that of the ExtendedUndoManager.
 *
 * @author sky
 */
public class ExtendedUndoManager extends UndoManager {
    private boolean ignoreEdits;
    private Actions undoAction;
    private Actions redoAction;
    private boolean undoing;
    
    
    public ExtendedUndoManager() {
        undoAction = new Actions();
        redoAction = new Actions();
    }
    
    public void setIgnoreEdits(boolean ignoreEdits) {
        this.ignoreEdits = ignoreEdits;
    }
    
    public boolean getIgnoreEdits() {
        return ignoreEdits;
    }
    
    public Action getUndoAction() {
        return undoAction;
    }
    
    public Action getRedoAction() {
        return redoAction;
    }
    
    public void undo() throws CannotUndoException {
        undoing = true;
        try {
            super.undo();
        } finally {
            undoing = false;
        }
    }

    public void redo() throws CannotRedoException {
        undoing = true;
        try {
            super.redo();
        } finally {
            undoing = false;
        }
    }

    public boolean addEdit(UndoableEdit anEdit) {
        if (!undoing && !getIgnoreEdits()) {
            boolean result = super.addEdit(anEdit);
            updateActions();
            return result;
        }
        return false;
    }
    
    public void updateActions() {
        undoAction.updateEnabled();
        redoAction.updateEnabled();
    }
    

    
    private class Actions extends AbstractAction {
        Actions() {
            setEnabled(false);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (isUndo()) {
                undo();
            } else {
                redo();
            }
            undoAction.updateEnabled();
            redoAction.updateEnabled();
        }
        
        private boolean isUndo() {
            return (this == undoAction);
        }

        private void updateEnabled() {
            if (isUndo()) {
                setEnabled(canUndo());
            } else {
                setEnabled(canRedo());
            }
        }
    }
}
