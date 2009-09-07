/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package se.sics.kompics.master.swing.adapter;

import java.awt.Color;
import java.awt.Component;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import se.sics.kompics.master.swing.model.NodeEntry;

/**
 * ListCellRenderer for the PasswordEntrys. 
 *
 * @author sky
 */
public final class EntryListCellRenderer extends JPanel implements ListCellRenderer {
    private static final int LIST_CELL_ICON_SIZE = 36;
    
    private JLabel hostLabel;
//    private JLabel userLabel;
    
    public EntryListCellRenderer() {
        hostLabel = new JLabel(" ");
//        userLabel = new JLabel(" ");
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        GroupLayout.SequentialGroup hg = layout.createSequentialGroup();
        layout.setHorizontalGroup(hg);
        hg.
                addGroup(layout.createParallelGroup().
                addComponent(hostLabel, 10, 10, Integer.MAX_VALUE)
//                addComponent(userLabel, 10, 10, Integer.MAX_VALUE)
                );
        
        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.
                addGroup(layout.createSequentialGroup().
                addComponent(hostLabel)
//                addComponent(userLabel)
                        );
        
//        layout.linkSize(SwingConstants.VERTICAL, imageLabel, strengthBar);
        
        setOpaque(true);
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        NodeEntry entry = (NodeEntry)value;
        String host = entry.getHostname();
        if (host == null) {
            host = " ";
        }
        hostLabel.setText(host);
        if (isSelected) {
            adjustColors(list.getSelectionBackground(),
                    list.getSelectionForeground(), this, hostLabel); //, userLabel
        } else {
            adjustColors(list.getBackground(),
                    list.getForeground(), this, hostLabel); // , userLabel
        }
        return this;
    }
    
    private void adjustColors(Color bg, Color fg, Component...components) {
        for (Component c : components) {
            c.setForeground(fg);
            c.setBackground(bg);
        }
    }

    
}
