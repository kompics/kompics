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
package se.sics.kompics.launch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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

/**
 * The <code>ProcessOutputFrame</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: ProcessOutputFrame.java 268 2008-09-28 19:18:04Z Cosmin $
 */
@SuppressWarnings("serial")
public class ProcessOutputFrame extends JFrame {

	private static int WIDTH = 700;

	private static int HEIGHT = 510;

	private javax.swing.JPanel jContentPane = null;

	private JPanel logPanel = null;

	private JMenuBar menuBar = null;
	private JPanel inputPanel = null;

	private JTextArea logArea = null;
	private JScrollPane scrollPane;

	private String processId;
	private String command;

	private int count;

	private ProcessLauncher processLauncher;
	private Scenario launcher;

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
	public ProcessOutputFrame(ProcessLauncher processLauncher, String command,
			String processID, int count, Scenario launcher) {
		super();
		this.processLauncher = processLauncher;
		this.processId = processID;
		this.command = command;
		this.count = count;
		this.launcher = launcher;
		initialize();
	}

	private void initialize() {
		this.setSize(WIDTH, HEIGHT);
		this.setContentPane(getJContentPane());
		this.setTitle("Process " + processId + " - " + command);

		this.setJMenuBar(getMyJMenuBar());

		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = getSize();
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(
				getClass().getResource("kompics32.png")));
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosed(java.awt.event.WindowEvent e) {
				processLauncher.kill(false);
			}
		});
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}

		if (count < 2) {
			setSize(screenSize.width, screenSize.height - 20);
			setLocation(0, 0);
		} else if (count < 3) {
			init2Frames(screenSize, frameSize);
		} else if (count < 6) {
			init4Frames(screenSize, frameSize);
		} else {
			init6Frames(screenSize, frameSize);
		}
	}

	private void init2Frames(Dimension screenSize, Dimension frameSize) {
		WIDTH = screenSize.width / 2;
		HEIGHT = (screenSize.height - 20);
		this.setSize(WIDTH, HEIGHT);

		if (processId.equals("1")) {
			setLocation(0, 0);
		} else if (processId.equals("2")) {
			setLocation(WIDTH, 0);
		} else {
			setLocation(WIDTH / 2, HEIGHT / 2);
		}
	}

	private void init4Frames(Dimension screenSize, Dimension frameSize) {
		WIDTH = screenSize.width / 2;
		HEIGHT = (screenSize.height - 20) / 2;
		this.setSize(WIDTH, HEIGHT);

		if (processId.equals("1")) {
			setLocation(0, 0);
		} else if (processId.equals("2")) {
			setLocation(WIDTH, 0);
		} else if (processId.equals("3")) {
			setLocation(0, HEIGHT);
		} else if (processId.equals("4")) {
			setLocation(WIDTH, HEIGHT);
		} else {
			setLocation(WIDTH / 2, HEIGHT / 2);
		}
	}

	private void init6Frames(Dimension screenSize, Dimension frameSize) {
		WIDTH = screenSize.width / 3;
		HEIGHT = (screenSize.height - 20) / 2;
		this.setSize(WIDTH, HEIGHT);

		if (processId.equals("1")) {
			setLocation(0, 0);
		} else if (processId.equals("2")) {
			setLocation(WIDTH, 0);
		} else if (processId.equals("3")) {
			setLocation(2 * WIDTH, 0);
		} else if (processId.equals("4")) {
			setLocation(0, HEIGHT);
		} else if (processId.equals("5")) {
			setLocation(WIDTH, HEIGHT);
		} else if (processId.equals("6")) {
			setLocation(2 * WIDTH, HEIGHT);
		} else if (processId.equals("7")) {
			setLocation(WIDTH / 2, HEIGHT / 2);
		} else if (processId.equals("8")) {
			setLocation(WIDTH * 3 / 2, HEIGHT / 2);
		} else {
			setLocation(WIDTH, HEIGHT / 2);
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
						launcher.killAll();
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
						processLauncher.kill(false);
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
			final JTextField textField = new JTextField();
			inputPanel.add(textField, BorderLayout.CENTER);
			textField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						processLauncher.input(e.getActionCommand());
						append(e.getActionCommand() + "\n");
						textField.setText("");
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
				}
			});

		}
		return inputPanel;
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
