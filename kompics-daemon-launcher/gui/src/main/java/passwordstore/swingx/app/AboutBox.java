/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx.app;

import java.awt.Window;
import java.util.MissingResourceException;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Simple about box implementation. Pulls name and icon from resources.
 */
public final class AboutBox {
    private static AboutBox INSTANCE;
    
    public static AboutBox getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AboutBox();
        }
        return INSTANCE;
    }
    
    AboutBox() {
    }
    
    public void show(Window parent) {
        JDialog dialog;
        if (parent == null) {
            dialog = new JDialog();
        } else {
            dialog = new JDialog(parent);
        }
        JLabel backgroundLabel = new javax.swing.JLabel();
        JLabel applicationNameLabel = new javax.swing.JLabel();
        JLabel developerNameLabel = new javax.swing.JLabel();
        JPanel content = new JPanel();
        dialog.setContentPane(content);
        content.setBackground(new java.awt.Color(90, 9, 6));
        String imageLoc;
        try {
            imageLoc = Application.getResourceAsString("aboutBox.loc");
        } catch (MissingResourceException mre) {
            imageLoc = null;
        }
        if (imageLoc == null) {
            imageLoc = "aboutBox-background.jpg";
        }
        backgroundLabel.setIcon(new javax.swing.ImageIcon(
                getClass().getResource(imageLoc)));
        
        applicationNameLabel.setFont(new java.awt.Font("Arial", 1, 20));
        applicationNameLabel.setForeground(new java.awt.Color(255, 255, 255));
        applicationNameLabel.setText(Application.getInstance().getName());
        
        developerNameLabel.setFont(new java.awt.Font("Arial", 0, 14));
        developerNameLabel.setForeground(new java.awt.Color(255, 255, 255));
        
        GroupLayout layout = new GroupLayout(content);
        content.setLayout(layout);
        layout.setHorizontalGroup(
          layout.createParallelGroup()
            .addComponent(applicationNameLabel, GroupLayout.Alignment.CENTER)
            .addComponent(backgroundLabel)
            );
        layout.setVerticalGroup(
          layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
              .addComponent(backgroundLabel, GroupLayout.PREFERRED_SIZE, 309, GroupLayout.PREFERRED_SIZE)
              .addComponent(applicationNameLabel)
              .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
        dialog.setTitle(Application.getResourceAsString("aboutBox.title"));
        dialog.setModal(true);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.show();
        dialog.dispose();
    }
}
