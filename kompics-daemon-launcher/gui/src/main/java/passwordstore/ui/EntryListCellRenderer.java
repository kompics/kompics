/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;

import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import passwordstore.model.NodeEntry;
import passwordstore.swingx.ImageCache;

/**
 * ListCellRenderer for the PasswordEntrys. 
 *
 * @author sky
 */
final class EntryListCellRenderer extends JPanel implements ListCellRenderer {
    private static final int LIST_CELL_ICON_SIZE = 36;
    
    private JLabel hostLabel;
    private JLabel userLabel;
    private JLabel imageLabel;
    private VerticalPasswordStrengthBar strengthBar;
    
    EntryListCellRenderer() {
        hostLabel = new JLabel(" ");
        userLabel = new JLabel(" ");
        imageLabel = new JLabel();
        imageLabel.setOpaque(true);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setBackground(Color.WHITE);
        int imageSize = LIST_CELL_ICON_SIZE + 4;
        imageLabel.setBorder(new CompoundBorder(
                new LineBorder(Color.BLACK, 1),
                new EmptyBorder(1, 1, 1, 1)));
        strengthBar = new VerticalPasswordStrengthBar();
        strengthBar.setOpaque(true);
        strengthBar.setBackground(Color.WHITE);
        strengthBar.setBorder(new LineBorder(Color.BLACK));
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        GroupLayout.SequentialGroup hg = layout.createSequentialGroup();
        layout.setHorizontalGroup(hg);
        hg.
                addComponent(imageLabel, imageSize, imageSize, imageSize).
                addGroup(layout.createParallelGroup().
                addComponent(hostLabel, 10, 10, Integer.MAX_VALUE).
                addComponent(userLabel, 10, 10, Integer.MAX_VALUE)).
                addComponent(strengthBar, 18, 18, 18);
        
        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.
                addComponent(imageLabel, GroupLayout.Alignment.CENTER, imageSize, imageSize, imageSize).
                addGroup(layout.createSequentialGroup().
                addComponent(hostLabel).
                addComponent(userLabel)).
                addComponent(strengthBar);
        
        layout.linkSize(SwingConstants.VERTICAL, imageLabel, strengthBar);
        
        setOpaque(true);
    }
    
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
    	NodeEntry entry = (NodeEntry)value;
        String host = entry.getHost();
        String user = entry.getUser();
        if (host == null) {
            host = " ";
        }
        if (user == null) {
            user = " ";
        }
        hostLabel.setText(host);
        userLabel.setText(user);
        imageLabel.setIcon(getImageIcon(entry, LIST_CELL_ICON_SIZE));
        if (entry != null) {
            strengthBar.setStrength(PasswordStrengthAnalyzer.getInstanceStrength(
                    entry.getPassword()));
        }
        if (isSelected) {
            adjustColors(list.getSelectionBackground(),
                    list.getSelectionForeground(), this, hostLabel, userLabel);
        } else {
            adjustColors(list.getBackground(),
                    list.getForeground(), this, hostLabel, userLabel);
        }
        return this;
    }
    
    private void adjustColors(Color bg, Color fg, Component...components) {
        for (Component c : components) {
            c.setForeground(fg);
            c.setBackground(bg);
        }
    }

    private Icon getImageIcon(NodeEntry entry, int size) {
        if (entry.getImagePath() != null) {
            Image image = ImageCache.getInstance().getImage(
                    this, entry.getImagePath(), size, size);
            if (image != null) {
                return new ImageIcon(image);
            }
        }
        return null;
    }
}
