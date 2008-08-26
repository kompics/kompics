package se.sics.kompics.management.jconsole;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.management.MBeanServerConnection;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import se.sics.kompics.management.ComponentMXBean;

@SuppressWarnings("serial")
public class KompicsTab extends JPanel implements TreeSelectionListener {

	private MBeanServerConnection server;

	private JButton refreshButton = null;

	private JSplitPane splitPane = null;
	private JPanel bottomPane = null;

	private JPanel leftPane = null;
	private JPanel rightPane = null;

	private JTree tree = null;
	private KompicsTreeModel treeModel = null;

	public KompicsTab() {
		super(new BorderLayout());
		add(getSplitPane(), BorderLayout.CENTER);
		add(getBottomPane(), BorderLayout.SOUTH);
	}

	// Set the MBeanServerConnection object for communicating
	// with the target VM
	public void setMBeanServerConnection(MBeanServerConnection mbs) {
		this.server = mbs;
		refresh();
	}

	public SwingWorker<KompicsTreeNode, Void> newSwingWorker() {
		return new KompicsTabSwingWorker(getTreeModel(), server);
	}

	private void refresh() {
		KompicsTabSwingWorker worker = new KompicsTabSwingWorker(
				getTreeModel(), server);
		worker.execute();
	}

	public void valueChanged(TreeSelectionEvent event) {
		KompicsTreeNode node = (KompicsTreeNode) getTree()
				.getLastSelectedPathComponent();
		if (node == null)
			return;

		ComponentMXBean cBean = node.getComponentMXBean();
		if (cBean != null) {
			ComponentPanel cPanel = new ComponentPanel();
			cPanel.setData(node.toString(), cBean.getCounters());
			getRightPane().removeAll();
			getRightPane().add(cPanel, BorderLayout.CENTER);
			getRightPane().validate();
		}
	}

	/* ============================== GRAPHICS ============================== */
	private JSplitPane getSplitPane() {
		if (splitPane == null) {
			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					getLeftPane(), getRightPane());
			splitPane.setDividerLocation(300);
			splitPane.setOneTouchExpandable(false);

			// Provide minimum sizes for the two components in the split pane
			Dimension minimumSize = new Dimension(100, 50);
			getLeftPane().setMinimumSize(minimumSize);
			getRightPane().setMinimumSize(minimumSize);
		}
		return splitPane;
	}

	private JPanel getBottomPane() {
		if (bottomPane == null) {
			bottomPane = new JPanel();
			bottomPane.add(getRefreshButton());
		}
		return bottomPane;
	}

	private JButton getRefreshButton() {
		if (refreshButton == null) {
			refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refresh();
				}
			});
		}
		return refreshButton;
	}

	private JPanel getLeftPane() {
		if (leftPane == null) {
			leftPane = new JPanel(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(getTree(),
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			leftPane.add(scrollPane, BorderLayout.CENTER);
		}
		return leftPane;
	}

	private JTree getTree() {
		if (tree == null) {
			tree = new JTree(getTreeModel());
			tree.getSelectionModel().setSelectionMode(
					TreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.addTreeSelectionListener(this);
		}
		return tree;
	}

	private KompicsTreeModel getTreeModel() {
		if (treeModel == null) {
			treeModel = new KompicsTreeModel(null);
		}
		return treeModel;
	}

	private JPanel getRightPane() {
		if (rightPane == null) {
			rightPane = new JPanel(new BorderLayout());
		}
		return rightPane;
	}
}
