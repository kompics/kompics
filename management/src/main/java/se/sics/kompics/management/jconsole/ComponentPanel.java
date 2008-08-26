package se.sics.kompics.management.jconsole;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import se.sics.kompics.management.ComponentEventCounter;

@SuppressWarnings("serial")
public class ComponentPanel extends JPanel {

	private JTable eventTable = null;

	private DefaultTableModel tableModel = null;

	private TitledBorder border;

	public ComponentPanel() {
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(getEventTable(),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);
		border = BorderFactory.createTitledBorder("Component");
		border.setTitleJustification(TitledBorder.CENTER);
		setBorder(border);
	}

	private JTable getEventTable() {
		if (eventTable == null) {
			eventTable = new JTable(getTableModel());
		}
		return eventTable;
	}

	private TableModel getTableModel() {
		return tableModel;
	}

	public void setData(String name, ComponentEventCounter[] counters) {
		Object data[][] = new Object[counters.length][];
		for (int i = 0; i < counters.length; i++) {
			data[i] = new Object[3];
			data[i][0] = counters[i].getEvent();
			data[i][1] = counters[i].getPublished();
			data[i][2] = counters[i].getHandled();
		}
		tableModel = new DefaultTableModel(data, new String[] { "Event type",
				"Published", "Handled" });
		getEventTable().setModel(tableModel);
		border.setTitle("Component: " + name);
	}
}
