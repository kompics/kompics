package se.sics.kompics.management.jconsole;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

public class KompicsTabSwingWorker extends SwingWorker<Object, Object> {

	private JLabel label;

	public KompicsTabSwingWorker(JLabel label) {
		super();
		this.label = label;
	}

	@Override
	protected Object doInBackground() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	protected void done() {
		label.setText("Time is now: " + System.currentTimeMillis());
	}
}
