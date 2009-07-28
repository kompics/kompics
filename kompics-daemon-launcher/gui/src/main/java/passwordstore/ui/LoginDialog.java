package passwordstore.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

class LoginDialog extends JDialog implements ActionListener,
		PropertyChangeListener {
	private String enteredSliceName = null;
	private String enteredPassword = null;
	
	private JTextField sliceNameField;
	private JTextField passwordField;
	
	private JLabel sliceName;
	private JLabel password;

	private JOptionPane userPane;
	private JOptionPane passwordPane;

	private String btnString1 = "Login";
	private String btnString2 = "Don't Login";

	/**
	 * Returns null if the typed string was invalid; otherwise, returns the
	 * string as the user entered it.
	 */
	public String getValidatedText() {
		return enteredSliceName;
	}

	/** Creates the reusable dialog. */
	public LoginDialog(JFrame aFrame, String aWord) {
		super(aFrame, true);

		setTitle("PlanetLab Login");
		
		sliceName = new JLabel("slicename");
		password = new JLabel("password");

		sliceNameField = new JTextField(30);
		passwordField = new JTextField(30);

		// Create an array of the text and components to be displayed.
		String msgString1 = "Planetlab Details";
		Object[] array = { msgString1, sliceName, sliceNameField, password, passwordField};

		// Create an array specifying the number of dialog buttons
		// and their text.
		Object[] options = { btnString1, btnString2 };

		// Create the JOptionPane.
		userPane = new JOptionPane(array, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION, null, options, options[0]);

		// Make this dialog display it.
		setContentPane(userPane);

		// Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				userPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});

		// Ensure the text field always gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				sliceNameField.requestFocusInWindow();
			}
		});

		// Register an event handler that puts the text into the option pane.
		sliceNameField.addActionListener(this);
		
		passwordField.addActionListener(this);

		// Register an event handler that reacts to option pane state changes.
		userPane.addPropertyChangeListener(this);
	}

	/** This method handles events for the text field. */
	public void actionPerformed(ActionEvent e) {
		userPane.setValue(btnString1);
	}

	/** This method reacts to state changes in the option pane. */
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible()
				&& (e.getSource() == userPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY
						.equals(prop))) {
			Object value = userPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				return;
			}

			// Reset the JOptionPane's value.
			// If you don't do this, then if the user
			// presses the same button next time, no
			// property change event will be fired.
			userPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

			if (btnString1.equals(value)) {
				enteredSliceName = sliceNameField.getText();
				enteredPassword = passwordField.getText();
				
//					// we're done; clear and dismiss the dialog
//					clearAndHide();
					// text was invalid
					sliceNameField.selectAll();
					passwordField.selectAll();
					JOptionPane.showMessageDialog(LoginDialog.this,
							"Sorry, \"" + enteredSliceName + "\" "
									+ "couldn't be logged in.\n",
							"Try again", JOptionPane.ERROR_MESSAGE);
					enteredSliceName = null;
					enteredPassword = null;
					sliceNameField.requestFocusInWindow();
			} else { // user closed dialog or clicked cancel
				enteredSliceName = null;
				enteredPassword = null;
				clearAndHide();
			}
		}
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		sliceNameField.setText(null);
		passwordField.setText(null);
		setVisible(false);
	}
}
