package se.sics.kompics.management.jconsole;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

@SuppressWarnings("serial")
public class KompicsTreeModel extends DefaultTreeModel {

	public KompicsTreeModel(TreeNode root) {
		super(root);
	}
}
