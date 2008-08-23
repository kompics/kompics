package se.sics.kompics.management.jconsole;

import java.awt.BorderLayout;

import javax.management.MBeanServerConnection;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class KompicsTab extends JPanel {

	private MBeanServerConnection server;

	private JLabel label;

	public KompicsTab() {
		super(new BorderLayout());
		label = new JLabel("new");
		add(label);
	}

	// Set the MBeanServerConnection object for communicating
	// with the target VM
	public void setMBeanServerConnection(MBeanServerConnection mbs) {
		this.server = mbs;
	}

	public SwingWorker<?, ?> newSwingWorker() {
		return new KompicsTabSwingWorker(label);
	}

}
