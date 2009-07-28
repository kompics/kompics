/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.swingx.text;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Utilities;

/**
 * RegExStyler styles a JTextComponent based on regular expressions.
 */
public class RegExStyler implements DocumentListener, MouseMotionListener {
    private final JTextComponent editor;
    private final DefaultStyledDocument doc;
    private final List<RegExStyle> stylesStorage;
    private DocumentEvent event;
    private Position minScanPosition;
    private Position maxScanPosition;
    private boolean pendingScan;

    public RegExStyler(JTextComponent editor) {
        stylesStorage = new ArrayList<RegExStyle>(10);
	this.editor = editor;
	doc = (DefaultStyledDocument)editor.getDocument();
	editor.addMouseMotionListener(this);
	doc.addDocumentListener(this);
    }
    
    /**
     * Adds a style.
     *
     * @param pattern the regular expression to search against
     * @param attribute the set of attributes to apply when a match is found
     * @param cursor the cursor to show when over a matching string
     */
    public void addStyle(String pattern, AttributeSet attribute, Cursor cursor) {
	addStyle(new RegExStyle(pattern,attribute,cursor));
    }
    
    /**
     * Adds a style.
     *
     * @param style specifies the style to apply
     */
    public void addStyle(RegExStyle style) {
	stylesStorage.add(style);
    }
    
    /**
     * Returns the list of styles.
     *
     * @return the list of styles
     */
    public List<RegExStyle> getStyles() {
        return Collections.unmodifiableList(stylesStorage);
    }
    
    /**
     * Returns the string that matches the specified style.
     *
     * @param e the MouseEvent to determine the location from
     * @param style the Style to search for
     */
    public String getMatchingText(MouseEvent e, RegExStyle style) {
        return getMatchingText(editor.viewToModel(e.getPoint()), style);
    }
    
    /**
     * Returns the string that matches the specified style at the specified
     * offset, or null if style does not match a string at the specified offset.
     *
     * @param offset the offset into the model
     * @param style the style to search against
     * @return the matching string, or null
     */
    public String getMatchingText(int offset, RegExStyle style) {
        int start = getParagraphStart(offset);
        int end = getParagraphEnd(offset);
        try {
            String text = doc.getText(start, end - start - 1);
            Matcher matcher = style.pattern.matcher(text);
            while (matcher.find()) {
                if (matcher.start() <= offset && matcher.end() >= offset) {
                    return text.substring(matcher.start(), matcher.end());
                }
            }
        } catch (BadLocationException ble) {
            assert false;
        }
        return null;
    }

    /**
     * A style.
     */
    public static final class RegExStyle {
	private final Pattern pattern;
	private final AttributeSet attribute;
	private final Cursor cursor;

        public RegExStyle(String pattern, AttributeSet attribute, Cursor cursor) {
	    this.pattern = Pattern.compile(pattern);
	    this.attribute = attribute; 
	    this.cursor = cursor;
	}
    }
    
    //document listener {
    public void insertUpdate(DocumentEvent e) {
        try {
            int start = e.getOffset();
            int end = e.getOffset() + e.getLength();
            if (minScanPosition == null) {
                minScanPosition = doc.createPosition(start);
                maxScanPosition = doc.createPosition(end);
            } else {
                if (start < minScanPosition.getOffset()) {
                    minScanPosition = doc.createPosition(start);
                }
                if (end > maxScanPosition.getOffset()) {
                    maxScanPosition = doc.createPosition(end);
                }
            }
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            assert false;
        }
        scheduleScan();
    }
    
    public void removeUpdate(DocumentEvent e) {
        try {
            int start = e.getOffset();
            if (minScanPosition == null) {
                minScanPosition = doc.createPosition(start);
                maxScanPosition = doc.createPosition(start);
            } else {
                if (start < minScanPosition.getOffset()) {
                    minScanPosition = doc.createPosition(start);
                }
                if (start > maxScanPosition.getOffset()) {
                    maxScanPosition = doc.createPosition(start);
                }
            }
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            assert false;
        }
        scheduleScan();
    }
    
    public void changedUpdate(DocumentEvent e) {
    }
    //document listener }    

    
    //mouseMotion listener {

    public void mouseDragged(MouseEvent event) {
    }
    public void mouseMoved(MouseEvent event) {
	try {
	    Segment txt = new Segment();
	    int offset = editor.viewToModel(event.getPoint());
            int pStart = getParagraphStart(offset);
            int pEnd = getParagraphEnd(offset);
	    String str = doc.getText(pStart, pEnd - pStart - 1);
	    int i;
	    RegExStyle regExStyle;
	    for (i = stylesStorage.size() - 1; i >= 0; i--) {
		regExStyle = (RegExStyle)stylesStorage.get(i);
		Matcher matcher = regExStyle.pattern.matcher(str);
		while (matcher.find()) {
		    if (matcher.start() <= offset
			&& offset < matcher.end()) {
			editor.setCursor(regExStyle.cursor);
			i = -100; //we want to break the loop
			break;
		    }
		}
	    }
	    if (i == -1) { //we did not have any matches
		editor.setCursor(null);
	    }  
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	
    }
    //mouseMotion listener }

    private void scheduleScan() {
        if (!pendingScan) {
            pendingScan = true;
            SwingUtilities.invokeLater(doColoring);
        }
    }
    
    private Runnable doColoring = 
	new Runnable () {
	    public void run() {
                pendingScan = false;
                int start = Math.min(doc.getLength(), minScanPosition.getOffset());
                int end = Math.min(doc.getLength(), maxScanPosition.getOffset());
                minScanPosition = maxScanPosition = null;
                int offset = getParagraphStart(start);
                AttributeSet emptyAttrs = new SimpleAttributeSet();
                while (offset < end) {
                    int nextOffset = getParagraphEnd(offset);
                    doc.setCharacterAttributes(offset, nextOffset - offset - 1, emptyAttrs, true);
                    String str = null;
                    try {
                        str = doc.getText(offset, nextOffset - offset);
                    } catch (BadLocationException ex) {
                        assert false;
                        ex.printStackTrace();
                    }
                    RegExStyle regExStyle;
                    for (RegExStyle style : stylesStorage) {
                        Matcher matcher = style.pattern.matcher(str);
                        while (matcher.find()) {
                            doc.setCharacterAttributes(offset + matcher.start(),
                                    matcher.end() - matcher.start(),
                                    style.attribute, true);
                        }
                    }
                    offset = nextOffset;
                }
	    }
	    
	};
    private String getRow(int offset) throws BadLocationException {
	Segment txt = new Segment();
	int rowStart, rowEnd;
	rowStart = Utilities.getRowStart(editor, offset);
	rowEnd   = Utilities.getRowEnd(editor, offset);
	doc.getText(rowStart, rowEnd - rowStart, txt);
	return txt.toString();
    }
    
    private int getParagraphStart(int offset) {
        return doc.getParagraphElement(offset).getStartOffset();
    }
    
    private int getParagraphEnd(int offset) {
        return doc.getParagraphElement(offset).getEndOffset();
    }
}
