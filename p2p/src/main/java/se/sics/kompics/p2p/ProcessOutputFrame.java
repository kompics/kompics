package se.sics.kompics.p2p;

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
 * The <code>ProcessOutputFrame</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: ProcessOutputFrame.java 76 2008-05-14 12:11:14Z cosmin $
 */
@SuppressWarnings("serial")
public class ProcessOutputFrame extends JFrame {

	private static int WIDTH = 700;

	private static int HEIGHT = 510;

	private javax.swing.JPanel jContentPane = null;

	private JPanel logPanel = null;

	// private JPanel commandPanel = null;
	//
	// private JButton killButton = null;

	private JMenuBar menuBar = null;
	private JPanel inputPanel = null;

	private JTextArea logArea = null;
	private JScrollPane scrollPane;

	// private JLabel commandLabel = null;
	//
	// private String command;

	private String name;

	private int pid;

	private ProcessLauncher processLauncher;

	public ProcessOutputFrame(ProcessLauncher processLauncher, String command,
			String name, int pid) {
		super();
		this.processLauncher = processLauncher;
		// this.command = command;
		this.name = name;
		this.pid = pid;
		initialize();
	}

	private void initialize() {
		this.setSize(WIDTH, HEIGHT);
		this.setContentPane(getJContentPane());
		this.setTitle(name);

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

		WIDTH = screenSize.width / 2;
		HEIGHT = (screenSize.height - 20) / 2;
		this.setSize(WIDTH, HEIGHT);

		if (pid == 0) {
			setLocation(0, 0);
		} else if (pid == 1) {
			setLocation(WIDTH, 0);
		} else if (pid == 2) {
			setLocation(0, HEIGHT);
		} else if (pid == 3) {
			setLocation(WIDTH, HEIGHT);
		} else {
			setLocation((screenSize.width - frameSize.width) / 2,
					(screenSize.height - frameSize.height) / 2);
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

			JMenuItem killAll = new JMenuItem("Kill all");
			killAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("Kill all")) {
						P2pLauncher.killAll();
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

			JMenuItem interrupt = new JMenuItem("Send interrupt (SIGINT)");
			interrupt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("Send interrupt (SIGINT)")) {
						processLauncher.kill(false);
					}
				}
			});
			interrupt.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
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
			// jContentPane.add(getCommandPanel(), BorderLayout.NORTH);
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
			JTextField textField = new JTextField();
			inputPanel.add(textField, BorderLayout.CENTER);
			textField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						processLauncher.input(e.getActionCommand());
						append(e.getActionCommand() + "\n");
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
				}
			});

		}
		return inputPanel;
	}

	// private JPanel getCommandPanel() {
	// if (commandPanel == null) {
	// commandPanel = new JPanel();
	// commandPanel.setLayout(new BorderLayout());
	// commandPanel
	// .setBorder(javax.swing.BorderFactory
	// .createTitledBorder(
	// javax.swing.BorderFactory
	// .createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED),
	// " Application component commands ",
	// javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
	// javax.swing.border.TitledBorder.DEFAULT_POSITION,
	// new java.awt.Font("Dialog",
	// java.awt.Font.BOLD, 12),
	// java.awt.Color.black));
	// JScrollPane scrollPane = new JScrollPane(getCommandLabel());
	// scrollPane
	// .setHorizontalScrollBarPolicy(ScrollPaneConstants.
	// HORIZONTAL_SCROLLBAR_ALWAYS);
	// commandPanel.add(scrollPane, BorderLayout.CENTER);
	// commandPanel.add(getKillButton(), BorderLayout.EAST);
	// }
	// return commandPanel;
	// }
	//
	// private JButton getKillButton() {
	// if (killButton == null) {
	// killButton = new JButton("Kill all");
	// killButton.addActionListener(new ActionListener() {
	// public void actionPerformed(ActionEvent e) {
	// if (e.getActionCommand().equals("Kill all")) {
	// P2pLauncher.killAll();
	// }
	// }
	// });
	// }
	// return killButton;
	// }

	private JPanel getLogPanel() {
		if (logPanel == null) {
			logPanel = new JPanel();
			logPanel.setLayout(new BorderLayout());
			// logPanel
			// .setBorder(javax.swing.BorderFactory
			// .createTitledBorder(
			// javax.swing.BorderFactory
			// .createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED),
			// " Process " + name + " ",
			// javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
			// javax.swing.border.TitledBorder.DEFAULT_POSITION,
			// new java.awt.Font("Dialog",
			// java.awt.Font.BOLD, 12),
			// java.awt.Color.black));
			scrollPane = new JScrollPane(getLogArea());
			logPanel.add(scrollPane, BorderLayout.CENTER);
		}
		return logPanel;
	}

	// private JLabel getCommandLabel() {
	// if (commandLabel == null) {
	// commandLabel = new JLabel(" " + command);
	// commandLabel.setAutoscrolls(true);
	// commandLabel.setFont(new Font("Courier New", Font.BOLD, 14));
	// }
	// return commandLabel;
	// }

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

	public void append(String string) {
		getLogArea().append(string);

		int length = getLogArea().getDocument().getLength();
		getLogArea().setCaretPosition(length);
	}
}
