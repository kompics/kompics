package se.sics.kompics.master.gui;

import javax.swing.JMenuItem;
/**
 * An example that shows a JToolbar, as well as a JList, JTable, JSplitPane and JTree
 */
public class MoreSwingComponents extends javax.swing.JFrame {
	private javax.swing.JPanel ivjJFrameContentPane = null;
	private javax.swing.JToolBar ivjJToolBar = null;
	private javax.swing.JButton ivjJButton = null;
	private javax.swing.JButton ivjJButton1 = null;
	private javax.swing.JButton ivjJButton2 = null;
	private javax.swing.JPanel ivjJPanel = null;
	private javax.swing.JScrollPane ivjJScrollPane = null;
	private javax.swing.JList ivjJList = null;
	private javax.swing.JScrollPane ivjJScrollPane1 = null;
	private javax.swing.JTable ivjJTable = null;
	private javax.swing.JComboBox ivjJComboBox = null;
	private javax.swing.JSplitPane ivjJSplitPane = null;
	private javax.swing.JButton ivjJButton3 = null;
	private javax.swing.JButton ivjJButton4 = null;
	private javax.swing.JMenuBar ivjJMenuBar = null;
	private javax.swing.JMenu ivjJMenu = null;
	private javax.swing.JMenu ivjJMenu1 = null;
	private javax.swing.JMenuItem ivjJMenuItem = null;
	private javax.swing.JMenuItem ivjJMenuItem1 = null;
	private javax.swing.JCheckBoxMenuItem ivjJCheckBoxMenuItem = null;
	private javax.swing.JRadioButtonMenuItem ivjJRadioButtonMenuItem = null;
	private javax.swing.JRadioButtonMenuItem ivjJRadioButtonMenuItem1 = null;
	private javax.swing.JMenu ivjJMenu2 = null;
	private javax.swing.JMenuItem ivjJMenuItem2 = null;
	private JMenuItem ivjJMenuItem3 = null;

	public MoreSwingComponents() {
		super();
		initialize();
	}

	/**
	 * Return the JFrameContentPane property value.
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJFrameContentPane() {
		if (ivjJFrameContentPane == null) {
			ivjJFrameContentPane = new javax.swing.JPanel();
			java.awt.BorderLayout layBorderLayout_3 = new java.awt.BorderLayout();
			ivjJFrameContentPane.setLayout(layBorderLayout_3);
			ivjJFrameContentPane.add(getIvjJToolBar(),
					java.awt.BorderLayout.NORTH);
			ivjJFrameContentPane.add(getIvjJPanel(),
					java.awt.BorderLayout.CENTER);
			ivjJFrameContentPane.setName("JFrameContentPane");
		}
		return ivjJFrameContentPane;
	}

	/**
	 * Initialize the class.
	 */
	private void initialize() {
		this.setContentPane(getJFrameContentPane());
		this.setJMenuBar(getIvjJMenuBar());
		this.setName("JFrame1");
		this.setTitle("More Swing Components");
		this
				.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		this.setBounds(23, 36, 526, 301);
	}

	/**
	 * This method initializes ivjJToolBar
	 * 
	 * @return javax.swing.JToolBar
	 */
	private javax.swing.JToolBar getIvjJToolBar() {
		if (ivjJToolBar == null) {
			ivjJToolBar = new javax.swing.JToolBar();
			ivjJToolBar.add(getIvjJButton());
			ivjJToolBar.add(getIvjJButton1());
			ivjJToolBar.add(getIvjJButton2());
			ivjJToolBar.add(getIvjJComboBox());
		}
		return ivjJToolBar;
	}

	/**
	 * This method initializes ivjJButton
	 * 
	 * @return javax.swing.JButton
	 */
	private javax.swing.JButton getIvjJButton() {
		if (ivjJButton == null) {
			ivjJButton = new javax.swing.JButton();
			ivjJButton.setText("One");
		}
		return ivjJButton;
	}

	/**
	 * This method initializes ivjJButton1
	 * 
	 * @return javax.swing.JButton
	 */
	private javax.swing.JButton getIvjJButton1() {
		if (ivjJButton1 == null) {
			ivjJButton1 = new javax.swing.JButton();
			ivjJButton1.setText("Two");
		}
		return ivjJButton1;
	}

	/**
	 * This method initializes ivjJButton2
	 * 
	 * @return javax.swing.JButton
	 */
	private javax.swing.JButton getIvjJButton2() {
		if (ivjJButton2 == null) {
			ivjJButton2 = new javax.swing.JButton();
			ivjJButton2.setText("Three");
		}
		return ivjJButton2;
	}

	/**
	 * This method initializes ivjJPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getIvjJPanel() {
		if (ivjJPanel == null) {
			ivjJPanel = new javax.swing.JPanel();
			java.awt.GridLayout layGridLayout_4 = new java.awt.GridLayout();
			layGridLayout_4.setRows(2);
			layGridLayout_4.setColumns(5);
			ivjJPanel.setLayout(layGridLayout_4);
			ivjJPanel.add(getIvjJScrollPane(), null);
			ivjJPanel.add(getIvjJScrollPane1(), null);
			ivjJPanel.add(getIvjJSplitPane(), null);
		}
		return ivjJPanel;
	}

	/**
	 * This method initializes ivjJScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private javax.swing.JScrollPane getIvjJScrollPane() {
		if (ivjJScrollPane == null) {
			ivjJScrollPane = new javax.swing.JScrollPane();
			ivjJScrollPane.setViewportView(getIvjJList());
			ivjJScrollPane
					.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			ivjJScrollPane
					.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		}
		return ivjJScrollPane;
	}

	/**
	 * This method initializes ivjJList
	 * 
	 * @return javax.swing.JList
	 */
	private javax.swing.JList getIvjJList() {
		if (ivjJList == null) {
			ivjJList = new javax.swing.JList();
		}
		return ivjJList;
	}

	/**
	 * This method initializes ivjJTable
	 * 
	 * @return javax.swing.JTable
	 */
	private javax.swing.JTable getIvjJTable() {
		if (ivjJTable == null) {
			ivjJTable = new javax.swing.JTable();
		}
		return ivjJTable;
	}

	/**
	 * This method initializes ivjJScrollPane1
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private javax.swing.JScrollPane getIvjJScrollPane1() {
		if (ivjJScrollPane1 == null) {
			ivjJScrollPane1 = new javax.swing.JScrollPane();
			ivjJScrollPane1.setViewportView(getIvjJTable());
		}
		return ivjJScrollPane1;
	}

	/**
	 * This method initializes ivjJComboBox
	 * 
	 * @return javax.swing.JComboBox
	 */
	private javax.swing.JComboBox getIvjJComboBox() {
		if (ivjJComboBox == null) {
			ivjJComboBox = new javax.swing.JComboBox();
		}
		return ivjJComboBox;
	}

	/**
	 * This method initializes ivjJSplitPane
	 * 
	 * @return javax.swing.JSplitPane
	 */
	private javax.swing.JSplitPane getIvjJSplitPane() {
		if (ivjJSplitPane == null) {
			ivjJSplitPane = new javax.swing.JSplitPane();
			ivjJSplitPane.setLeftComponent(getIvjJButton3());
			ivjJSplitPane.setRightComponent(getIvjJButton4());
		}
		return ivjJSplitPane;
	}

	/**
	 * This method initializes ivjJButton3
	 * 
	 * @return javax.swing.JButton
	 */
	private javax.swing.JButton getIvjJButton3() {
		if (ivjJButton3 == null) {
			ivjJButton3 = new javax.swing.JButton();
			ivjJButton3.setText("Left Button");
		}
		return ivjJButton3;
	}

	/**
	 * This method initializes ivjJButton4
	 * 
	 * @return javax.swing.JButton
	 */
	private javax.swing.JButton getIvjJButton4() {
		if (ivjJButton4 == null) {
			ivjJButton4 = new javax.swing.JButton();
			ivjJButton4.setText("Right Button");
		}
		return ivjJButton4;
	}

	/**
	 * This method initializes ivjJMenuBar
	 * 
	 * @return javax.swing.JMenuBar
	 */
	private javax.swing.JMenuBar getIvjJMenuBar() {
		if (ivjJMenuBar == null) {
			ivjJMenuBar = new javax.swing.JMenuBar();
			ivjJMenuBar.add(getIvjJMenu());
			ivjJMenuBar.add(getIvjJMenu1());
		}
		return ivjJMenuBar;
	}

	/**
	 * This method initializes ivjJMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private javax.swing.JMenu getIvjJMenu() {
		if (ivjJMenu == null) {
			ivjJMenu = new javax.swing.JMenu();
			ivjJMenu.add(getIvjJMenuItem());
			ivjJMenu.add(getIvjJMenuItem1());
			ivjJMenu.add(getIvjJMenu2());
			ivjJMenu.setText("File");
			ivjJMenu.add(getIvjJMenuItem3());
		}
		return ivjJMenu;
	}

	/**
	 * This method initializes ivjJMenu1
	 * 
	 * @return javax.swing.JMenu
	 */
	private javax.swing.JMenu getIvjJMenu1() {
		if (ivjJMenu1 == null) {
			ivjJMenu1 = new javax.swing.JMenu();
			ivjJMenu1.add(getIvjJCheckBoxMenuItem());
			ivjJMenu1.add(getIvjJRadioButtonMenuItem());
			ivjJMenu1.add(getIvjJRadioButtonMenuItem1());
			ivjJMenu1.setText("Window");
		}
		return ivjJMenu1;
	}

	/**
	 * This method initializes ivjJMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private javax.swing.JMenuItem getIvjJMenuItem() {
		if (ivjJMenuItem == null) {
			ivjJMenuItem = new javax.swing.JMenuItem();
			ivjJMenuItem.setText("First");
		}
		return ivjJMenuItem;
	}

	/**
	 * This method initializes ivjJMenuItem1
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private javax.swing.JMenuItem getIvjJMenuItem1() {
		if (ivjJMenuItem1 == null) {
			ivjJMenuItem1 = new javax.swing.JMenuItem();
			ivjJMenuItem1.setText("Second");
		}
		return ivjJMenuItem1;
	}

	/**
	 * This method initializes ivjJCheckBoxMenuItem
	 * 
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private javax.swing.JCheckBoxMenuItem getIvjJCheckBoxMenuItem() {
		if (ivjJCheckBoxMenuItem == null) {
			ivjJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
			ivjJCheckBoxMenuItem.setText("is Active");
		}
		return ivjJCheckBoxMenuItem;
	}

	/**
	 * This method initializes ivjJRadioButtonMenuItem
	 * 
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private javax.swing.JRadioButtonMenuItem getIvjJRadioButtonMenuItem() {
		if (ivjJRadioButtonMenuItem == null) {
			ivjJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
			ivjJRadioButtonMenuItem.setText("On");
		}
		return ivjJRadioButtonMenuItem;
	}

	/**
	 * This method initializes ivjJRadioButtonMenuItem1
	 * 
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private javax.swing.JRadioButtonMenuItem getIvjJRadioButtonMenuItem1() {
		if (ivjJRadioButtonMenuItem1 == null) {
			ivjJRadioButtonMenuItem1 = new javax.swing.JRadioButtonMenuItem();
			ivjJRadioButtonMenuItem1.setText("Off");
		}
		return ivjJRadioButtonMenuItem1;
	}

	/**
	 * This method initializes ivjJMenu2
	 * 
	 * @return javax.swing.JMenu
	 */
	private javax.swing.JMenu getIvjJMenu2() {
		if (ivjJMenu2 == null) {
			ivjJMenu2 = new javax.swing.JMenu();
			ivjJMenu2.add(getIvjJMenuItem2());
			ivjJMenu2.setText("Sub Menu");
		}
		return ivjJMenu2;
	}

	/**
	 * This method initializes ivjJMenuItem2
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private javax.swing.JMenuItem getIvjJMenuItem2() {
		if (ivjJMenuItem2 == null) {
			ivjJMenuItem2 = new javax.swing.JMenuItem();
			ivjJMenuItem2.setText("Third");
		}
		return ivjJMenuItem2;
	}

	/**
	 * This method initializes ivjJMenuItem3	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getIvjJMenuItem3() {
		if (ivjJMenuItem3 == null) {
			ivjJMenuItem3 = new JMenuItem();
			ivjJMenuItem3.setText("First");
		}
		return ivjJMenuItem3;
	}
} //  @jve:visual-info  decl-index=0 visual-constraint="0,0"
