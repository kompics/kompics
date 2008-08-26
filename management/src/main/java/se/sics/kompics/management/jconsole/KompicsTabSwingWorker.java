package se.sics.kompics.management.jconsole;

import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.swing.SwingWorker;

import se.sics.kompics.management.ChannelMXBean;
import se.sics.kompics.management.ComponentMXBean;
import se.sics.kompics.management.KompicsMXBean;

public class KompicsTabSwingWorker extends SwingWorker<KompicsTreeNode, Void> {

	private KompicsTreeModel treeModel = null;

	private MBeanServerConnection server = null;

	public KompicsTabSwingWorker(KompicsTreeModel treeModel,
			MBeanServerConnection server) {
		super();
		this.treeModel = treeModel;
		this.server = server;
	}

	@Override
	protected KompicsTreeNode doInBackground() throws Exception {
		ComponentMXBean root = getBootstrapComponent();
		return buildKompicsTree(root);
	}

	protected void done() {
		try {
			KompicsTreeNode root = get();
			treeModel.setRoot(root);
			treeModel.nodeStructureChanged(root);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private KompicsTreeNode buildKompicsTree(ComponentMXBean component) {
		ComponentMXBean[] subComponents = component.getChildren();
		ChannelMXBean[] channels = component.getChannels();
		Vector<KompicsTreeNode> children;
		if (subComponents.length + channels.length > 0) {
			children = new Vector<KompicsTreeNode>();
		} else {
			children = null;
		}

		for (int i = 0; i < subComponents.length; i++) {
			KompicsTreeNode child = buildKompicsTree(subComponents[i]);
			children.add(child);
		}
		for (int i = 0; i < channels.length; i++) {
			children.add(new KompicsTreeNode(channels[i], null));
		}

		KompicsTreeNode node = new KompicsTreeNode(component, children);
		if (children != null) {
			for (KompicsTreeNode child : children) {
				child.setParent(node);
			}
		}

		return node;
	}

	private ComponentMXBean getBootstrapComponent() {
		try {
			ObjectName kompicsMbeanName = new ObjectName(
					"se.sics.kompics:type=Kompics");
			KompicsMXBean kompicsMXBean = JMX.newMXBeanProxy(server,
					kompicsMbeanName, KompicsMXBean.class);
			return kompicsMXBean.getBootstrapComponent();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}
}
