/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.text;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import passwordstore.swingx.Utilities;

/**
 * TextUndoableEditGenerator generates UndoableEdit events from a
 * JTextComponent. JTextComponent's generate UndoableEditEdits out of the
 * box, but these are not useful once you've invoked setText. The events
 * generates by this class can be used even after you've completely changed
 * the text.
 * <p>
 * TextUndoableEditGenerator works by installing a DocumentFilter on the
 * JTextComponent. As JFormattedTextField installs a DocumentFilter,
 * this class will not work with a JFormattedTextField.
 *
 * @author sky
 */
public final class TextUndoableEditGenerator {
    private enum MutationType {
        INSERT, REMOVE, REPLACE
    }
    
    private final List<UndoableEditListener> listeners;
    private final JTextComponent textComponent;
    private boolean mutating;
    private Edit lastEdit;
    
    public TextUndoableEditGenerator(JTextComponent text) {
        listeners = new CopyOnWriteArrayList<UndoableEditListener>();
        this.textComponent = text;
        // PENDING: this needs to listen for changes to the document
        ((AbstractDocument)text.getDocument()).setDocumentFilter(
                new DocumentFilterImpl());
        textComponent.addCaretListener(new CaretHandler());
    }
    
    public JTextComponent getTextComponent() {
        return textComponent;
    }
    
    private Document getDocument() {
        return getTextComponent().getDocument();
    }

    private boolean isMutating() {
        return mutating;
    }

    private void caretChanged() {
        if (!isMutating() && lastEdit != null) {
            lastEdit.end();
            lastEdit = null;
        }
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

    private void accumulateEdit(int offset, int length, String text) {
        if (length > 0) {
            accumulateEdit(new Edit(offset, length, false));
        }
        if (text != null && text.length() > 0) {
            accumulateEdit(new Edit(offset, text.length(), true));
        }
    }
    
    public void addUndoableEditListener(UndoableEditListener uel) {
        listeners.add(uel);
    }
    
    public void removeUndoableEditListener(UndoableEditListener uel) {
        listeners.remove(uel);
    }
    
    private void fireUndoableEditHappened(UndoableEdit edit) {
        fireUndoableEditHappened(new UndoableEditEvent(this, edit));
    }
    
    private void fireUndoableEditHappened(UndoableEditEvent event) {
        for (UndoableEditListener l : listeners) {
            l.undoableEditHappened(event);
        }
    }
    
    

    private final class DocumentFilterImpl extends DocumentFilter {
        public void replace(DocumentFilter.FilterBypass fb, int offset,
                int length, String text, AttributeSet attrs) throws BadLocationException {
            lastEdit = null;
            mutating = true;
            accumulateEdit(offset, length, text);
            super.replace(fb, offset, length, text, attrs);
            mutating = false;
            fireUndoableEditHappened(lastEdit);
        }

        public void remove(DocumentFilter.FilterBypass fb,
                int offset, int length) throws BadLocationException {
            lastEdit = null;
            mutating = true;
            accumulateEdit(offset, length, null);
            super.remove(fb, offset, length);
            mutating = false;
            fireUndoableEditHappened(lastEdit);
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset,
                String string, AttributeSet attr) throws BadLocationException {
            lastEdit = null;
            mutating = true;
            accumulateEdit(offset, 0, string);
            super.insertString(fb, offset, string, attr);
            mutating = false;
            fireUndoableEditHappened(lastEdit);
        }
    }
    
    private class Edit extends AbstractUndoableEdit {
        private boolean ended;
        private List<Edit> edits;
        private int offset;
        private int length;
        private String text;
        private boolean isInsert;
        
        Edit(int offset, int length, boolean isInsert) {
            this.offset = offset;
            this.length = length;
            if (!isInsert) {
                try {
                    text = getDocument().getText(offset, length);
                } catch (BadLocationException ex) {
                }
            }
            if (length > 1) {
                end();
            }
            this.isInsert = isInsert;
        }

        public void end() {
            ended = true;
        }

        private boolean hasEnded() {
            return ended;
        }
        
        private int getOffset() {
            return offset;
        }
        
        private int getLength() {
            return length;
        }
        
        private boolean isInsert() {
            return isInsert;
        }
        
        private String getText() {
            return text;
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (!hasEnded()) {
                if (anEdit instanceof Edit &&
                        incorporateEdit((Edit)anEdit)) {
                    return true;
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
        
        protected boolean incorporateEdit(Edit edit) {
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
                Utilities.makeVisible(textComponent);
                textComponent.requestFocus();
                if (isUndo == isInsert()) {
                    text = getDocument().getText(offset, length);
                    getDocument().remove(offset, length);
                    getTextComponent().setCaretPosition(offset);
                } else {
                    getDocument().insertString(offset, text, null);
                    getTextComponent().select(offset, offset + length);
                }
            } catch (BadLocationException ex) {
            }
        }

        public String toString() {
            return "Edit [offset=" + offset +
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
