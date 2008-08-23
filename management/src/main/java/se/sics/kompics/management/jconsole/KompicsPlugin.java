package se.sics.kompics.management.jconsole;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.jconsole.JConsoleContext.ConnectionState;

public class KompicsPlugin extends JConsolePlugin implements
		PropertyChangeListener {
	private KompicsTab kompicsTab = null;
	private Map<String, JPanel> tabs = null;

	public KompicsPlugin() {
		// register itself as a listener
		addContextPropertyChangeListener(this);
	}

	/*
	 * Returns a Kompics tab to be added in JConsole.
	 */
	public synchronized Map<String, JPanel> getTabs() {
		if (tabs == null) {
			kompicsTab = new KompicsTab();
			kompicsTab.setMBeanServerConnection(getContext()
					.getMBeanServerConnection());
			// use LinkedHashMap if you want a predictable order
			// of the tabs to be added in JConsole
			tabs = new LinkedHashMap<String, JPanel>();
			tabs.put("Kompics", kompicsTab);
		}
		return tabs;
	}

	/*
	 * Returns a SwingWorker which is responsible for updating the Kompics tab.
	 */
	public SwingWorker<?, ?> newSwingWorker() {
		return kompicsTab.newSwingWorker();
	}

	// You can implement the dispose() method if you need to release
	// any resource when the plugin instance is disposed when the JConsole
	// window is closed.
	//
	// public void dispose() {
	// }

	/*
	 * Property listener to reset the MBeanServerConnection at reconnection
	 * time.
	 */
	public void propertyChange(PropertyChangeEvent ev) {
		String prop = ev.getPropertyName();
		if (prop == JConsoleContext.CONNECTION_STATE_PROPERTY) {
			ConnectionState newState = (ConnectionState) ev.getNewValue();
			// JConsole supports disconnection and reconnection
			// The MBeanServerConnection will become invalid when
			// disconnected. Need to use the new MBeanServerConnection object
			// created at reconnection time.
			if (newState == ConnectionState.CONNECTED && kompicsTab != null) {
				kompicsTab.setMBeanServerConnection(getContext()
						.getMBeanServerConnection());
			}
		}
	}

}
