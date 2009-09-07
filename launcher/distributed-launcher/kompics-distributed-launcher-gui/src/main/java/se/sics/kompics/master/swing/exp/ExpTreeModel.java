/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.master.swing.exp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.address.Address;
import se.sics.kompics.wan.ssh.Host;

/**
 *
 * @author jdowling
 */
public class ExpTreeModel extends DefaultTreeModel implements PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ExpTreeModel.class);

    public ExpTreeModel(TreeNode treeNode) { 
        super(treeNode);
    }

    public void propertyChange(PropertyChangeEvent e) {

//        if (e.getSource() instanceof ExpEntry) {
//            entryChanged((ExpEntry) e.getSource(), e.getPropertyName(),
//                (ExpEntry) e.getOldValue());
//        }
        reload();
    }

//    private void entryChanged(ExpEntry expEntry,
//            String propertyChanged, ExpEntry lastValue) {
//
//        if (propertyChanged.compareTo(ExpEntry.ExperimentStatus.class.getCanonicalName())==0)
//        {
//            reload();
//        }
//        reload();
//    }
}
