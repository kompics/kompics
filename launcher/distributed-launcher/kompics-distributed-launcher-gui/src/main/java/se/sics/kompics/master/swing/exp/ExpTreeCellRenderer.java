/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.master.swing.exp;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jdowling
 */
public class ExpTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ExpTreeCellRenderer.class);

    JLabel nodeLabel;
    JLabel expStatusLabel;
    JLabel iconLabel;
    JPanel renderer;
    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    Color backgroundSelectionColor;
    Color backgroundNonSelectionColor;
    ImageIcon statusIcon = createImageIcon("images/middle.gif");

    

    public ExpTreeCellRenderer() {
        renderer = new JPanel(new GridLayout(0, 2));
        backgroundSelectionColor = defaultRenderer.getBackgroundSelectionColor();
        backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();

        nodeLabel = new JLabel("");
        expStatusLabel = new JLabel("");
        iconLabel = new JLabel(statusIcon);
        nodeLabel.setForeground(Color.blue);
        renderer.add(nodeLabel);
        renderer.add(expStatusLabel);
        renderer.add(iconLabel);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            if (userObject instanceof ExpEntry) {
                ExpEntry expEntry = (ExpEntry) userObject;

                if (expEntry.isDaemon()) {
                    nodeLabel.setText(expEntry.getAddress().toString());
                    iconLabel.setIcon(statusIcon);
                    expStatusLabel.setText(expEntry.getStatus().toString());
                    expStatusLabel.setForeground(Color.CYAN);
                } else {
                    nodeLabel.setText(Integer.toString(expEntry.getAddress().getId()));
                    iconLabel.setIcon(null);
                    expStatusLabel.setText("");
                    // expStatusLabel.setText(expEntry.getStatus().toString());
                    expStatusLabel.setForeground(Color.RED);
                }                
                
                

                if (selected) {
                    renderer.setBackground(backgroundSelectionColor);
                } else {
                    renderer.setBackground(backgroundNonSelectionColor);
                }
                renderer.setEnabled(tree.isEnabled());
                returnValue = renderer;


            }
        }
        if (returnValue == null) {
            logger.warn("Using default tree rederer for experiments");
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree,
                    value, selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }

    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = ExpTreeCellRenderer.class.getResource(path);

        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            logger.warn("Couldn't find file: " + path);
            return null;
        }
    }
}
