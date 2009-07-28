/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.text;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * @author sky
 */
public final class ExtendedPlainDocument extends PlainDocument {
    private enum MutationType {
        INSERT, REMOVE, REPLACE
    }
    
    private boolean mutating;
    private JTextComponent textComponent;
    private Edit lastEdit;
    
    public ExtendedPlainDocument(JTextComponent text) {
        this.textComponent = text;
        textComponent.setDocument(this);
        textComponent.addCaretListener(new CaretHandler());
    }

    public void replace(int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (!mutating) {
            mutate(MutationType.REPLACE, offset, length, text, attrs);
        } else {
            super.replace(offset, length, text, attrs);
        }
    }
    
    public void insertString(int offset, String text, AttributeSet attrs)
            throws BadLocationException {
        if (!mutating) {
            mutate(MutationType.INSERT, offset, 0, text, attrs);
        } else {
            super.insertString(offset, text, attrs);
        }
    }

    public void remove(int offset, int length) throws BadLocationException {
        if (!mutating) {
            mutate(MutationType.REMOVE, offset, length, null, null);
        } else {
            super.remove(offset, length);
        }
    }
    
    public boolean isMutating() {
        return mutating;
    }

    private void mutate(MutationType type, int offset, int length,
            String text, AttributeSet attrs) throws BadLocationException {
        lastEdit = null;
        try {
            mutating = true;
            switch(type) {
                case INSERT:
                    super.insertString(offset, text, attrs);
                    break;
                case REMOVE:
                    super.remove(offset, length);
                    break;
                case REPLACE:
                    super.replace(offset, length, text, attrs);
                    break;
            }
            fireUndoableEditUpdate0(new UndoableEditEvent(this, lastEdit));
        } finally {
            mutating = false;
        }
    }
    
    private void caretChanged() {
        if (!isMutating() && lastEdit != null) {
            lastEdit.end();
            lastEdit = null;
        }
    }

    
    
    // Invoked before the remove.
    protected void removeUpdate(DefaultDocumentEvent event) {
        int offset = event.getOffset();
        int length = event.getLength();
        accumulateEdit(new Edit(offset, length, false));
        super.removeUpdate(event);
    }
    
    // Invoked after the insert
    protected void insertUpdate(DefaultDocumentEvent event, AttributeSet attr) {
        accumulateEdit(new Edit(event.getOffset(), event.getLength(), true));
        super.insertUpdate(event, attr);
    }

    private void accumulateEdit(Edit edit) {
        if (lastEdit != null) {
            lastEdit.addEdit0(edit);
            // Force multi-edits (replace) to only contain the insert/remove
            // pair.
            lastEdit.end();
        } else {
            lastEdit = edit;
        }
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
    }
    
    protected void fireUndoableEditUpdate0(UndoableEditEvent e) {
        super.fireUndoableEditUpdate(e);
    }
    

    private class Edit extends AbstractUndoableEdit {
        private boolean ended;
        private List<Edit> edits;
        private boolean isInsert;
        private int offset;
        private int length;
        private String text;
        
        public Edit(int offset, int length, boolean isInsert) {
            this.offset = offset;
            this.length = length;
            if (!isInsert) {
                try {
                    text = ExtendedPlainDocument.this.getText(offset, length);
                } catch (BadLocationException ex) {
                }
            }
            if (length > 1) {
                end();
            }
            this.isInsert = isInsert;
        }
        
        public String getText() {
            return text;
        }
        
        public void end() {
            ended = true;
        }
        
        private boolean hasEnded() {
            return ended;
        }
        
        private boolean isInsert() {
            return isInsert;
        }
        
        private int getOffset() {
            return offset;
        }
        
        private int getLength() {
            return length;
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (!ended) {
                if (anEdit instanceof Edit) {
                    if (incorporateEdit((Edit)anEdit)) {
                        return true;
                    }
                }
                end();
            } else if (edits != null) {
                return edits.get(edits.size() - 1).addEdit(anEdit);
            }
            return false;
        }
        
        private void addEdit0(Edit edit) {
            if (edits == null) {
                edits = new LinkedList<Edit>();
            }
            edits.add(edit);
        }
        
        private boolean incorporateEdit(Edit edit) {
            if (!edit.hasEnded() && edit.getLength() == 1) {
                if (isInsert() && edit.isInsert()) {
                    if (getOffset() + getLength() == edit.getOffset()) {
                        length += edit.getLength();
                        return true;
                    }
                } else if (!isInsert() && !edit.isInsert()) {
                    if (getOffset() == edit.getOffset()) {
                        // forward delete
                        text += edit.getText();
                        length++;
                        return true;
                    } else if (getOffset() - 1 == edit.getOffset()) {
                        // backward delete
                        text = edit.getText() + text;
                        offset--;
                        length++;
                        return true;
                    }
                }
            }
            return false;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            end();
            undoSubedits();
            reapplyEdit(true);
        }

        public void redo() throws CannotRedoException {
            super.redo();
            end();
            reapplyEdit(false);
            redoSubedits();
        }

        private void undoSubedits() {
            if (edits != null) {
                for (int i = edits.size() - 1; i >= 0; i--) {
                    edits.get(i).undo();
                }
            }
        }

        private void redoSubedits() {
            if (edits != null) {
                for (Edit edit : edits) {
                    edit.redo();
                }
            }
        }

        private void reapplyEdit(boolean isUndo) {
            try {
                int offset = getOffset();
                int length = getLength();
                textComponent.requestFocus();
                if (isUndo == isInsert()) {
                    text = ExtendedPlainDocument.this.getText(offset, length);
                    remove(offset, length);
                    textComponent.setCaretPosition(offset);
                } else {
                    insertString(offset, text, null);
                    textComponent.select(offset, offset + length);
                }
            } catch (BadLocationException ex) {
            }
        }
        
        public String toString() {
            return "Edit[offset=" + offset +
                    ", length=" + length +
                    ", isInsert=" + isInsert +
                    " edits=[" + edits + "]]";
        }
    }
    
    
    private class CaretHandler implements CaretListener {
        public void caretUpdate(CaretEvent e) {
            caretChanged();
        }
    }
}
