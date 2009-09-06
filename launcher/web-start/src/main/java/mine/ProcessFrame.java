/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import mine.ProcLauncher.Proc;

/**
 * The <code>ProcessFrame</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: ProcessFrame.java 1148 2009-09-01 23:30:59Z Cosmin $
 */
@SuppressWarnings("serial")
public class ProcessFrame extends JFrame {

	private static int WIDTH = 700;

	private static int HEIGHT = 510;

	private javax.swing.JPanel jContentPane = null;

	private JPanel logPanel = null;

	private JMenuBar menuBar = null;
	private JPanel inputPanel = null;
	private JTextField localInputTextField = null;
	private JTextField globalInputTextField = null;

	private JTextArea logArea = null;
	private JScrollPane scrollPane;

	private int idx;

	private int count;

	private Proc proc;
	
	private String processId;

	/**
	 * Instantiates a new process output frame.
	 * 
	 * @param processLauncher
	 *            the process launcher
	 * @param command
	 *            the command
	 * @param processID
	 *            the process id
	 * @param count
	 *            the count
	 * @param launcher
	 *            the launcher
	 */
	public ProcessFrame(Proc proc, int idx, int count) {
		super();
		this.proc = proc;
		this.count = count;
		this.idx = idx;
		String names[] = proc.getMainComponent().split("\\.");
		this.processId = names[names.length - 1];
		initialize();
	}

	private void initialize() {
		this.setSize(WIDTH, HEIGHT);
		this.setContentPane(getJContentPane());
		this.setTitle("Process " + processId);

		this.setJMenuBar(getMyJMenuBar());

		// Center the window
		this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);
		this.setVisible(true);

		int left = this.getX();
		int top = this.getY();
		int width = this.getWidth();
		int height = this.getHeight();

		this.setIconImage(Toolkit.getDefaultToolkit().getImage(
				getClass().getResource("kompics32.png")));
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				proc.kill(false);
			}
		});
		this.addWindowFocusListener(new WindowAdapter() {
			public void windowGainedFocus(WindowEvent e) {
				getLocalInputTextField().requestFocusInWindow();
			}
		});

		if (count < 2) {
			setLocation(left, top);
		} else if (count < 3) {
			init2Frames(left, top, width, height);
		} else if (count < 6) {
			init4Frames(left, top, width, height);
		} else {
			init6Frames(left, top, width, height);
		}

		getLocalInputTextField().requestFocusInWindow();
	}

	private void init2Frames(int left, int top, int width, int height) {
		WIDTH = width / 2;
		HEIGHT = height;
		this.setSize(WIDTH, HEIGHT);

		if (idx == 1) {
			setLocation(left, top);
		} else if (idx == 2) {
			setLocation(left + WIDTH, top);
		} else {
			setLocation(left + WIDTH / 2, top + HEIGHT / 2);
		}
	}

	private void init4Frames(int left, int top, int width, int height) {
		WIDTH = width / 2;
		HEIGHT = height / 2;
		this.setSize(WIDTH, HEIGHT);

		if (idx == 1) {
			setLocation(left, top);
		} else if (idx == 2) {
			setLocation(left + WIDTH, top);
		} else if (idx == 3) {
			setLocation(left, top + HEIGHT);
		} else if (idx == 4) {
			setLocation(left + WIDTH, top + HEIGHT);
		} else {
			setLocation(left + WIDTH / 2, top + HEIGHT / 2);
		}
	}

	private void init6Frames(int left, int top, int width, int height) {
		WIDTH = width / 3;
		HEIGHT = height / 2;
		this.setSize(WIDTH, HEIGHT);

		if (idx == 1) {
			setLocation(left, top);
		} else if (idx == 2) {
			setLocation(left + WIDTH, top);
		} else if (idx == 3) {
			setLocation(left + 2 * WIDTH, top);
		} else if (idx == 4) {
			setLocation(left, top + HEIGHT);
		} else if (idx == 5) {
			setLocation(left + WIDTH, top + HEIGHT);
		} else if (idx == 6) {
			setLocation(left + 2 * WIDTH, top + HEIGHT);
		} else if (idx == 7) {
			setLocation(left + WIDTH / 2, top + HEIGHT / 2);
		} else if (idx == 8) {
			setLocation(left + WIDTH * 3 / 2, top + HEIGHT / 2);
		} else {
			setLocation(left + WIDTH, top + HEIGHT / 2);
		}
	}

	private javax.swing.JMenuBar getMyJMenuBar() {
		if (menuBar == null) {
			menuBar = new JMenuBar();

			JMenu terminal = new JMenu("Terminal");
			JMenu process = new JMenu("Process");
			// JMenu test = new JMenu("Test");

			menuBar.add(terminal);
			menuBar.add(process);
			// menuBar.add(Box.createHorizontalGlue());
			// menuBar.add(test);

			JMenuItem killAll = new JMenuItem("Kill all processes");
			killAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("Kill all processes")) {
						proc.killAll();
					}
				}
			});
			killAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
					ActionEvent.CTRL_MASK));
			process.add(killAll);

			JMenuItem copy = new JMenuItem("Copy all to clipboard");
			copy.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("Copy all to clipboard")) {
						Clipboard clipboard = Toolkit.getDefaultToolkit()
								.getSystemClipboard();
						clipboard.setContents(new StringSelection(getLogArea()
								.getText()), new ClipboardOwner() {
							public void lostOwnership(Clipboard clipboard,
									Transferable contents) {
							}
						});
					}
				}
			});
			copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
					ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

			JMenuItem interrupt = new JMenuItem("Kill process " + processId);
			interrupt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand()
							.equals("Kill process " + processId)) {
						proc.kill(false);
					}
				}
			});
			interrupt.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
					ActionEvent.CTRL_MASK));

			terminal.add(copy);
			process.addSeparator();
			process.add(interrupt);
		}

		return menuBar;
	}

	private javax.swing.JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new javax.swing.JPanel();
			jContentPane.setLayout(new java.awt.BorderLayout());
			jContentPane.add(getLogPanel(), BorderLayout.CENTER);
			jContentPane.add(getInputPanel(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	private JPanel getInputPanel() {
		if (inputPanel == null) {
			inputPanel = new JPanel();
			inputPanel.setLayout(new BorderLayout());
			inputPanel.add(new JLabel(" Input: "), BorderLayout.WEST);
			inputPanel.add(getLocalInputTextField(), BorderLayout.CENTER);
			inputPanel.add(getGlobalInputTextField(), BorderLayout.EAST);
		}
		return inputPanel;
	}

	private JTextField getLocalInputTextField() {
		if (localInputTextField == null) {
			localInputTextField = new JTextField();
			localInputTextField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						proc.input(e.getActionCommand());
						append(e.getActionCommand() + "\n");
						localInputTextField.setText("");
					} catch (IOException e1) {
					}
				}
			});
		}
		return localInputTextField;
	}

	private JTextField getGlobalInputTextField() {
		if (globalInputTextField == null) {
			globalInputTextField = new JTextField(20);
			globalInputTextField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						proc.globalInput(e.getActionCommand());
						globalInputTextField.setText("");
					} catch (IOException e1) {
					}
				}
			});
		}
		return globalInputTextField;
	}

	private JPanel getLogPanel() {
		if (logPanel == null) {
			logPanel = new JPanel();
			logPanel.setLayout(new BorderLayout());
			scrollPane = new JScrollPane(getLogArea());
			logPanel.add(scrollPane, BorderLayout.CENTER);
		}
		return logPanel;
	}

	private JTextArea getLogArea() {
		if (logArea == null) {
			logArea = new JTextArea(1, 80);
			logArea.setAutoscrolls(true);
			logArea.setEditable(false);
			logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
			logArea.setBackground(Color.DARK_GRAY);
			logArea.setForeground(Color.WHITE);
			logArea.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent e) {
					getLocalInputTextField().requestFocusInWindow();
				}
			});
		}
		return logArea;
	}

	/**
	 * Append.
	 * 
	 * @param string
	 *            the string
	 */
	public void append(String string) {
		getLogArea().append(string);

		int length = getLogArea().getDocument().getLength();
		getLogArea().setCaretPosition(length);
	}
}
