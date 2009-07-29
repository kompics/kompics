/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

import passwordstore.model.HostEntry;
import passwordstore.model.NodeEntry;
import passwordstore.swingx.Utilities;
import passwordstore.swingx.binding.ListController;

/**
 * An action for generating passwords.
 *
 * @author sky
 */
final class PasswordGeneratingAction extends AbstractAction {
    private final boolean generateUpper;
    private final boolean generateLower;
    private final boolean generateDigits;
    private final boolean generateOther;
    private final ListController<NodeEntry> controller;
    private final JTextField tf;
    
    public static final String generatePassword(int length, boolean upper,
            boolean lower, boolean digits, boolean punctuation) {
        if (!upper && !lower && !digits && !punctuation) {
            throw new IllegalArgumentException();
        }
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        char[] password = new char[length];
        int chunks = 0;
        if (upper) {
            chunks++;
        }
        if (lower) {
            chunks++;
        }
        if (digits) {
            chunks++;
        }
        if (punctuation) {
            chunks++;
        }
        int chunkSize = length / chunks;
        List<Integer> indices = generateIndices(length);
        assign(random, indices, password, upper, 'A', 26, chunkSize);
        assign(random, indices, password, lower, 'a', 26, chunkSize);
        assign(random, indices, password, digits, '0', 10, chunkSize);
        assign(random, indices, password, punctuation, "!@#$%^&*()", chunkSize);

        assign(random, indices, password, upper, 'A', 26, indices.size());
        assign(random, indices, password, lower, 'a', 26, indices.size());
        assign(random, indices, password, digits, '0', 10, indices.size());
        assign(random, indices, password, punctuation, "!@#$%^&*()", indices.size());
        return new String(password);
    }

    private static List<Integer> generateIndices(int length) {
        List<Integer> values = new ArrayList<Integer>(length);
        for (int i = 0; i < length; i++) {
            values.add(i);
        }
        Collections.shuffle(values);
        return values;
    }

    private static void assign(Random random, List<Integer> indices, char[] password,
            boolean add, char base, int length, int count) {
        if (add) {
            for (int i = 0; i < count; i++) {
                password[indices.remove(0)] = (char)(random.nextInt(length) + base);
            }
        }
    }

    private static void assign(Random random, List<Integer> indices,
            char[] password, boolean add, String string, int count) {
        if (add) {
            for (int i = 0; i < count; i++) {
                password[indices.remove(0)] = string.charAt(random.nextInt(string.length()));
            }
        }
    }

    public PasswordGeneratingAction(ListController<NodeEntry> controller,
            JTextField tf, boolean generateUpper, boolean generateLower,
            boolean generateDigits, boolean generateOther) {
        this.tf = tf;
        this.controller = controller;
        controller.addPropertyChangeListener(new ListControllerPropertyChangeListener());
        this.generateUpper = generateUpper;
        this.generateLower = generateLower;
        this.generateDigits = generateDigits;
        this.generateOther = generateOther;
        updateEnabled();
    }

    public void actionPerformed(ActionEvent e) {
    	NodeEntry entry = controller.getSelectedEntry();
        String password = generatePassword(8, generateUpper, generateLower,
                generateDigits, generateOther);
        entry.setPassword(password);
        Utilities.makeVisible(tf);
        tf.requestFocus();
        tf.selectAll();
    }

    private void updateEnabled() {
        setEnabled(controller.getSelection().size() == 1);
    }
    
    
    private final class ListControllerPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName() == "selection") {
                updateEnabled();
            }
        }
    }
}
