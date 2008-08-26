package se.sics.kompics.management.jconsole;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import se.sics.kompics.management.ChannelMXBean;
import se.sics.kompics.management.ComponentMXBean;

public class KompicsTreeNode implements TreeNode {

	private Vector<KompicsTreeNode> children;

	private KompicsTreeNode parent;

	private ComponentMXBean componentMXBean;

	private ChannelMXBean channelMXBean;

	private String name;

	public KompicsTreeNode(ComponentMXBean mbean,
			Vector<KompicsTreeNode> children) {
		super();
		this.componentMXBean = mbean;
		this.channelMXBean = null;
		this.name = mbean.getName();
		this.parent = null;
		if (children == null) {
			this.children = new Vector<KompicsTreeNode>();
		} else {
			this.children = children;
		}
	}

	public KompicsTreeNode(ChannelMXBean mbean, Vector<KompicsTreeNode> children) {
		super();
		this.componentMXBean = null;
		this.channelMXBean = mbean;
		this.parent = null;
		this.name = "Channel";
		if (children == null) {
			this.children = new Vector<KompicsTreeNode>();
		} else {
			this.children = children;
		}
	}

	public void setParent(KompicsTreeNode parent) {
		this.parent = parent;
	}

	public Enumeration<KompicsTreeNode> children() {
		return children.elements();
	}

	public boolean getAllowsChildren() {
		return componentMXBean != null;
	}

	public TreeNode getChildAt(int index) {
		return children.elementAt(index);
	}

	public int getChildCount() {
		return children.size();
	}

	public int getIndex(TreeNode node) {
		return children.indexOf(node);
	}

	public TreeNode getParent() {
		return parent;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public ComponentMXBean getComponentMXBean() {
		return componentMXBean;
	}

	public ChannelMXBean getChannelMXBean() {
		return channelMXBean;
	}

	public String toString() {
		return name;
	}
}
