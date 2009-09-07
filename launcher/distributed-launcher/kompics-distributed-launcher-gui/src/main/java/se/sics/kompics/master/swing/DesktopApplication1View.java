/*
 * DesktopApplication1View.java
 */
package se.sics.kompics.master.swing;

import se.sics.kompics.master.swing.exp.ExperimentWizardPanel2;
import se.sics.kompics.master.swing.exp.ExperimentWizardPanel1;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import javax.swing.event.ListDataEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;
import se.sics.kompics.master.swing.exp.ExpPane;
import se.sics.kompics.master.swing.exp.ExperimentWizardPanel3a;
import se.sics.kompics.master.swing.exp.ExperimentWizardPanel4a;
import se.sics.kompics.master.swing.exp.ExperimentWizardPanel4b;
import se.sics.kompics.wan.job.ArtifactJob;
import se.sics.kompics.master.swing.model.NodeEntry;
import se.sics.kompics.master.swing.model.UserEntry;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshCredentials;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;
import se.sics.kompics.wan.util.PomUtils;

/**
 * The application's main frame.
 */
public class DesktopApplication1View extends FrameView implements ListDataListener, PropertyChangeListener {

    private static final DataFlavor NODE_ENTRY_DATA_FLAVOR;
    private static final Logger logger = Logger.getLogger(DesktopApplication1View.class.getCanonicalName());
    Credentials cred = null;
    private Set<NodeEntry> selectedEntries = new HashSet<NodeEntry>();

    int experimentCounter = 0;

 

    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType +
                    "; class=java.util.ArrayList; x=password");
        } catch (ClassNotFoundException ex) {
            assert false;
        }
        NODE_ENTRY_DATA_FLAVOR = flavor;
    }

    public DesktopApplication1View(SingleFrameApplication app) {
        super(app);

//        getClient().addTableModelListener(this);

        getSClient().getListModel().addListDataListener(this);

        initComponents();


        this.hostList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent evt) {
                listSelectionChanged(evt);
            }
        });


        cred = getSClient().getCredentials();
        if (cred != null) {
            this.usernameLabel.setText(cred.getSshLoginName());
            this.sshKeyfileLabel.setText(cred.getKeyPath());
        }

        getSClient().getUserEntry().addPropertyChangeListener(this);

//        try {
//            getSClient().getNodes();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
//        }




        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }


    private Client getSClient() {
        return ((DesktopApplication1) getApplication()).getSClient();
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = DesktopApplication1.getApplication().getMainFrame();
            aboutBox = new DesktopApplication1AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        DesktopApplication1.getApplication().show(aboutBox);
    }

    @Action
    public void loginBox() {

        LoginDetailsEnterButton.setEnabled(false);
        DesktopApplication1.getApplication().show(loginDialog);
    }

    @Action
    public void publicKeyBox() {
        DesktopApplication1.getApplication().show(publicKeyDialog);
    }

    @Action
    public void importHostsBox() {
        DesktopApplication1.getApplication().show(ImportHostsDialog);
    }

    @Action
    public void addHostsBox() {
        DesktopApplication1.getApplication().show(AddHostsDialog);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        mainTabbedPane = new javax.swing.JTabbedPane();
        mainTabPanel = new javax.swing.JPanel();
        installDaemonButton = new javax.swing.JButton();
        removeHostButtonMain = new javax.swing.JButton();
        startDaemonButton = new javax.swing.JButton();
        installJavaButton = new javax.swing.JButton();
        addHostButtonMain = new javax.swing.JButton();
        updateHostsButton = new javax.swing.JButton();
        forceJavaInstallCheckBox = new javax.swing.JCheckBox();
        forceDaemonInstallCheckBox = new javax.swing.JCheckBox();
        stopDaemonButton = new javax.swing.JButton();
        nodeEntryMsgL = new javax.swing.JLabel();
        hostScrollPane = new javax.swing.JScrollPane();
        hostList = new javax.swing.JList();
        filterTextField = new javax.swing.JTextField();
        filterLabel = new javax.swing.JLabel();
        getDaemonLogsButton = new javax.swing.JButton();
        viewDaemonLogButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        accountMenu = new javax.swing.JMenu();
        planetLabAccountMenuItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        addHostsMenuItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        importHostsMI = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        DefineExperimentMenuItem = new javax.swing.JMenuItem();
        loadExperimentMenuItem = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        BackgroundTaskMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        usernameLabel = new javax.swing.JLabel();
        sshKeyfileLabel = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        accountPopupMenu = new javax.swing.JPopupMenu();
        loginDialog = new javax.swing.JDialog();
        javax.swing.JLabel appTitleLabel = new javax.swing.JLabel();
        LoginDetailsEnterButton = new javax.swing.JButton();
        loginCancelButton = new javax.swing.JButton();
        loginUsernameTextField = new javax.swing.JTextField();
        sliceTextField = new javax.swing.JTextField();
        planetLabUserPasswordField = new javax.swing.JPasswordField();
        selectPublicKeyButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        loginPublicKeyTextField = new javax.swing.JTextField();
        loginPublicKeyPasswordField = new javax.swing.JPasswordField();
        jLabel4 = new javax.swing.JLabel();
        loginNoPublicKeyCheckBox = new javax.swing.JCheckBox();
        loginErrorMsgLabel = new javax.swing.JLabel();
        publicKeyDialog = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();
        publicKeyFileChooser = new javax.swing.JFileChooser();
        AddHostsDialog = new javax.swing.JDialog();
        addHostButton = new javax.swing.JButton();
        exitAddinghostsButton = new javax.swing.JButton();
        hostnameField1 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        hostAddedLabel = new javax.swing.JLabel();
        ImportHostsDialog = new javax.swing.JDialog();
        hostsFileChooser = new javax.swing.JFileChooser();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        importedFileSelectedLabel = new javax.swing.JLabel();
        importHostsButton = new javax.swing.JButton();
        cancelImportHostsButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        loadJobDialog = new javax.swing.JDialog();
        LoadJobLabel = new javax.swing.JLabel();
        loadJobAsExperimentButton = new javax.swing.JButton();
        jobsComboBox = new javax.swing.JComboBox();
        daemonLogDialog = new javax.swing.JDialog();
        jScrollPane4 = new javax.swing.JScrollPane();
        daemonLogTextArea = new javax.swing.JTextArea();
        daemonLogLabel = new javax.swing.JLabel();
        exitDaemonLogsButton = new javax.swing.JButton();

        mainPanel.setFocusable(false);
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(768, 617));
        mainPanel.setRequestFocusEnabled(false);
        mainPanel.setVerifyInputWhenFocusTarget(false);

        mainTabbedPane.setName("mainTabbedPane"); // NOI18N

        mainTabPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        mainTabPanel.setName("hostsTab"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(se.sics.kompics.master.swing.DesktopApplication1.class).getContext().getResourceMap(DesktopApplication1View.class);
        installDaemonButton.setText(resourceMap.getString("installDaemonButton.text")); // NOI18N
        installDaemonButton.setName("installDaemonButton"); // NOI18N
        installDaemonButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installDaemonButtonActionPerformed(evt);
            }
        });

        removeHostButtonMain.setText(resourceMap.getString("removeHostButtonMain.text")); // NOI18N
        removeHostButtonMain.setName("removeHostButtonMain"); // NOI18N
        removeHostButtonMain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeHostButtonMainActionPerformed(evt);
            }
        });

        startDaemonButton.setText(resourceMap.getString("startDaemonButton.text")); // NOI18N
        startDaemonButton.setName("startDaemonButton"); // NOI18N
        startDaemonButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startDaemonButtonActionPerformed(evt);
            }
        });

        installJavaButton.setText(resourceMap.getString("installJavaButton.text")); // NOI18N
        installJavaButton.setName("installJavaButton"); // NOI18N
        installJavaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installJavaButtonActionPerformed(evt);
            }
        });

        addHostButtonMain.setText(resourceMap.getString("addHostButtonMain.text")); // NOI18N
        addHostButtonMain.setName("addHostButtonMain"); // NOI18N
        addHostButtonMain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHostButtonMainActionPerformed(evt);
            }
        });

        updateHostsButton.setText(resourceMap.getString("updateHostsButton.text")); // NOI18N
        updateHostsButton.setName("updateHostsButton"); // NOI18N
        updateHostsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateHostsButtonActionPerformed(evt);
            }
        });

        forceJavaInstallCheckBox.setText(resourceMap.getString("forceJavaInstallCheckBox.text")); // NOI18N
        forceJavaInstallCheckBox.setName("forceJavaInstallCheckBox"); // NOI18N

        forceDaemonInstallCheckBox.setSelected(true);
        forceDaemonInstallCheckBox.setText(resourceMap.getString("forceDaemonInstallCheckBox.text")); // NOI18N
        forceDaemonInstallCheckBox.setName("forceDaemonInstallCheckBox"); // NOI18N

        stopDaemonButton.setText(resourceMap.getString("stopDaemonButton.text")); // NOI18N
        stopDaemonButton.setName("stopDaemonButton"); // NOI18N
        stopDaemonButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopDaemonButtonActionPerformed(evt);
            }
        });

        nodeEntryMsgL.setBackground(resourceMap.getColor("nodeEntryMsgL.background")); // NOI18N
        nodeEntryMsgL.setFont(resourceMap.getFont("nodeEntryMsgL.font")); // NOI18N
        nodeEntryMsgL.setForeground(resourceMap.getColor("nodeEntryMsgL.foreground")); // NOI18N
        nodeEntryMsgL.setText(resourceMap.getString("nodeEntryMsgL.text")); // NOI18N
        nodeEntryMsgL.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        nodeEntryMsgL.setName("nodeEntryMsgL"); // NOI18N

        hostScrollPane.setName("hostScrollPane"); // NOI18N
        hostScrollPane.setPreferredSize(new java.awt.Dimension(600, 300));

        hostList.setModel(getSClient().getListModel());
        hostList.setCellRenderer(new NodeEntryListCellRenderer());
        hostList.setMinimumSize(new java.awt.Dimension(600, 300));
        hostList.setName("hostList"); // NOI18N
        hostList.setVisibleRowCount(15);
        hostScrollPane.setViewportView(hostList);

        filterTextField.setText(resourceMap.getString("filterTextField.text")); // NOI18N
        filterTextField.setEnabled(false);
        filterTextField.setName("filterTextField"); // NOI18N

        filterLabel.setText(resourceMap.getString("filterLabel.text")); // NOI18N
        filterLabel.setName("filterLabel"); // NOI18N

        getDaemonLogsButton.setText(resourceMap.getString("getDaemonLogsButton.text")); // NOI18N
        getDaemonLogsButton.setName("getDaemonLogsButton"); // NOI18N
        getDaemonLogsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getDaemonLogsButtonActionPerformed(evt);
            }
        });

        viewDaemonLogButton.setText(resourceMap.getString("viewDaemonLogButton.text")); // NOI18N
        viewDaemonLogButton.setEnabled(false);
        viewDaemonLogButton.setName("viewDaemonLogButton"); // NOI18N
        viewDaemonLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewDaemonLogButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout mainTabPanelLayout = new org.jdesktop.layout.GroupLayout(mainTabPanel);
        mainTabPanel.setLayout(mainTabPanelLayout);
        mainTabPanelLayout.setHorizontalGroup(
            mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(mainTabPanelLayout.createSequentialGroup()
                        .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.CENTER, installJavaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 129, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.CENTER, installDaemonButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 129, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(forceJavaInstallCheckBox)
                            .add(forceDaemonInstallCheckBox)))
                    .add(updateHostsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 128, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, viewDaemonLogButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, getDaemonLogsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, removeHostButtonMain, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, addHostButtonMain, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, stopDaemonButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, startDaemonButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)))
                .add(12, 12, 12)
                .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, mainTabPanelLayout.createSequentialGroup()
                        .add(432, 432, 432)
                        .add(filterLabel)
                        .add(24, 24, 24)
                        .add(filterTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, mainTabPanelLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(nodeEntryMsgL, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 687, Short.MAX_VALUE)
                            .add(hostScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 687, Short.MAX_VALUE))))
                .add(155, 155, 155))
        );
        mainTabPanelLayout.setVerticalGroup(
            mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(filterLabel)
                    .add(filterTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(29, 29, 29)
                .add(nodeEntryMsgL, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(mainTabPanelLayout.createSequentialGroup()
                        .add(38, 38, 38)
                        .add(updateHostsButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(forceDaemonInstallCheckBox)
                            .add(installDaemonButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mainTabPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(installJavaButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(forceJavaInstallCheckBox))
                        .add(18, 18, 18)
                        .add(startDaemonButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(stopDaemonButton)
                        .add(30, 30, 30)
                        .add(addHostButtonMain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeHostButtonMain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(getDaemonLogsButton)
                        .add(18, 18, 18)
                        .add(viewDaemonLogButton))
                    .add(mainTabPanelLayout.createSequentialGroup()
                        .add(27, 27, 27)
                        .add(hostScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)))
                .add(175, 175, 175))
        );

        mainTabbedPane.addTab(resourceMap.getString("hostsTab.TabConstraints.tabTitle"), mainTabPanel); // NOI18N

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1063, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 696, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(se.sics.kompics.master.swing.DesktopApplication1.class).getContext().getActionMap(DesktopApplication1View.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        accountMenu.setText(resourceMap.getString("accountMenu.text")); // NOI18N
        accountMenu.setName("accountMenu"); // NOI18N

        planetLabAccountMenuItem.setText(resourceMap.getString("planetLabAccountMenuItem.text")); // NOI18N
        planetLabAccountMenuItem.setName("planetLabAccountMenuItem"); // NOI18N
        planetLabAccountMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                planetLabAccountMenuItemActionPerformed(evt);
            }
        });
        accountMenu.add(planetLabAccountMenuItem);

        menuBar.add(accountMenu);

        jMenu3.setText(resourceMap.getString("jMenu3.text")); // NOI18N
        jMenu3.setEnabled(false);
        jMenu3.setName("jMenu3"); // NOI18N

        addHostsMenuItem.setText(resourceMap.getString("addHostsMenuItem.text")); // NOI18N
        addHostsMenuItem.setName("addHostsMenuItem"); // NOI18N
        addHostsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHostsMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(addHostsMenuItem);

        jMenu5.setText(resourceMap.getString("jMenu5.text")); // NOI18N
        jMenu5.setName("jMenu5"); // NOI18N

        jMenuItem11.setText(resourceMap.getString("jMenuItem11.text")); // NOI18N
        jMenuItem11.setName("jMenuItem11"); // NOI18N
        jMenu5.add(jMenuItem11);

        jMenu3.add(jMenu5);

        importHostsMI.setText(resourceMap.getString("importHostsMI.text")); // NOI18N
        importHostsMI.setName("importHostsMI"); // NOI18N
        importHostsMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importHostsMIActionPerformed(evt);
            }
        });
        jMenu3.add(importHostsMI);

        menuBar.add(jMenu3);

        jMenu2.setText(resourceMap.getString("jMenu2.text")); // NOI18N
        jMenu2.setEnabled(false);
        jMenu2.setName("jMenu2"); // NOI18N

        jMenuItem4.setText(resourceMap.getString("jMenuItem4.text")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        jMenu2.add(jMenuItem4);

        menuBar.add(jMenu2);

        jMenu4.setText(resourceMap.getString("jMenu4.text")); // NOI18N
        jMenu4.setName("jMenu4"); // NOI18N

        DefineExperimentMenuItem.setText(resourceMap.getString("DefineExperimentMenuItem.text")); // NOI18N
        DefineExperimentMenuItem.setName("DefineExperimentMenuItem"); // NOI18N
        DefineExperimentMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                DefineExperimentMenuItemMouseClicked(evt);
            }
        });
        DefineExperimentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DefineExperimentMenuItemActionPerformed(evt);
            }
        });
        DefineExperimentMenuItem.addMenuKeyListener(new javax.swing.event.MenuKeyListener() {
            public void menuKeyPressed(javax.swing.event.MenuKeyEvent evt) {
                DefineExperimentMenuItemMenuKeyPressed(evt);
            }
            public void menuKeyReleased(javax.swing.event.MenuKeyEvent evt) {
            }
            public void menuKeyTyped(javax.swing.event.MenuKeyEvent evt) {
            }
        });
        jMenu4.add(DefineExperimentMenuItem);

        loadExperimentMenuItem.setText(resourceMap.getString("loadExperimentMenuItem.text")); // NOI18N
        loadExperimentMenuItem.setEnabled(false);
        loadExperimentMenuItem.setName("loadExperimentMenuItem"); // NOI18N
        loadExperimentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadExperimentMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(loadExperimentMenuItem);

        jMenuItem5.setText(resourceMap.getString("jMenuItem5.text")); // NOI18N
        jMenuItem5.setEnabled(false);
        jMenuItem5.setName("jMenuItem5"); // NOI18N
        jMenu4.add(jMenuItem5);

        jMenuItem6.setText(resourceMap.getString("jMenuItem6.text")); // NOI18N
        jMenuItem6.setEnabled(false);
        jMenuItem6.setName("jMenuItem6"); // NOI18N
        jMenu4.add(jMenuItem6);

        jMenuItem7.setText(resourceMap.getString("jMenuItem7.text")); // NOI18N
        jMenuItem7.setEnabled(false);
        jMenuItem7.setName("jMenuItem7"); // NOI18N
        jMenu4.add(jMenuItem7);

        BackgroundTaskMenuItem.setText(resourceMap.getString("BackgroundTaskMenuItem.text")); // NOI18N
        BackgroundTaskMenuItem.setEnabled(false);
        BackgroundTaskMenuItem.setName("BackgroundTaskMenuItem"); // NOI18N
        BackgroundTaskMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BackgroundTaskMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(BackgroundTaskMenuItem);

        menuBar.add(jMenu4);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setEnabled(false);
        jMenu1.setName("jMenu1"); // NOI18N

        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenu1.add(jMenuItem3);

        jMenuItem8.setText(resourceMap.getString("jMenuItem8.text")); // NOI18N
        jMenuItem8.setName("jMenuItem8"); // NOI18N
        jMenu1.add(jMenuItem8);

        menuBar.add(jMenu1);

        statusPanel.setDebugGraphicsOptions(javax.swing.DebugGraphics.NONE_OPTION);
        statusPanel.setFocusCycleRoot(true);
        statusPanel.setFont(resourceMap.getFont("statusPanel.font")); // NOI18N
        statusPanel.setMaximumSize(new java.awt.Dimension(1200, 1200));
        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setOpaque(false);
        statusPanel.setPreferredSize(new java.awt.Dimension(561, 65));

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        usernameLabel.setText(resourceMap.getString("usernameLabel.text")); // NOI18N
        usernameLabel.setName("usernameLabel"); // NOI18N

        sshKeyfileLabel.setText(resourceMap.getString("sshKeyfileLabel.text")); // NOI18N
        sshKeyfileLabel.setName("sshKeyfileLabel"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(24, 24, 24)
                .add(usernameLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(60, 60, 60)
                        .add(jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(sshKeyfileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(600, 600, 600)
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusAnimationLabel)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                                .add(jLabel6)
                                .add(262, 262, 262))))
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(134, 134, 134)
                        .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 380, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .add(8, 8, 8)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(statusMessageLabel)
                        .add(statusAnimationLabel))
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(jLabel6)))
                .add(36, 36, 36))
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                        .add(1, 1, 1))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, usernameLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(29, 29, 29))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .add(progressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                .add(20, 20, 20)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sshKeyfileLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        accountPopupMenu.setName("accountPopupMenu"); // NOI18N

        loginDialog.setName("loginDialog"); // NOI18N

        appTitleLabel.setFont(appTitleLabel.getFont().deriveFont(appTitleLabel.getFont().getStyle() | java.awt.Font.BOLD, appTitleLabel.getFont().getSize()+4));
        appTitleLabel.setText(resourceMap.getString("appTitleLabel.text")); // NOI18N
        appTitleLabel.setName("appTitleLabel"); // NOI18N

        LoginDetailsEnterButton.setText(resourceMap.getString("LoginDetailsEnterButton.text")); // NOI18N
        LoginDetailsEnterButton.setName("LoginDetailsEnterButton"); // NOI18N
        LoginDetailsEnterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginDetailsEnterButtonActionPerformed(evt);
            }
        });

        loginCancelButton.setText(resourceMap.getString("loginCancelButton.text")); // NOI18N
        loginCancelButton.setName("loginCancelButton"); // NOI18N
        loginCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginCancelButtonActionPerformed(evt);
            }
        });

        loginUsernameTextField.setText(resourceMap.getString("loginUsernameTextField.text")); // NOI18N
        loginUsernameTextField.setName("loginUsernameTextField"); // NOI18N

        sliceTextField.setText(resourceMap.getString("sliceTextField.text")); // NOI18N
        sliceTextField.setName("sliceTextField"); // NOI18N

        planetLabUserPasswordField.setText(resourceMap.getString("planetLabUserPasswordField.text")); // NOI18N
        planetLabUserPasswordField.setName("planetLabUserPasswordField"); // NOI18N

        selectPublicKeyButton.setText(resourceMap.getString("selectPublicKeyButton.text")); // NOI18N
        selectPublicKeyButton.setName("selectPublicKeyButton"); // NOI18N
        selectPublicKeyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPublicKeyButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        loginPublicKeyTextField.setText(resourceMap.getString("loginPublicKeyTextField.text")); // NOI18N
        loginPublicKeyTextField.setName("loginPublicKeyTextField"); // NOI18N

        loginPublicKeyPasswordField.setText(resourceMap.getString("loginPublicKeyPasswordField.text")); // NOI18N
        loginPublicKeyPasswordField.setName("loginPublicKeyPasswordField"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        loginNoPublicKeyCheckBox.setText(resourceMap.getString("loginNoPublicKeyCheckBox.text")); // NOI18N
        loginNoPublicKeyCheckBox.setName("loginNoPublicKeyCheckBox"); // NOI18N
        loginNoPublicKeyCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginNoPublicKeyCheckBoxActionPerformed(evt);
            }
        });

        loginErrorMsgLabel.setBackground(resourceMap.getColor("loginErrorMsgLabel.background")); // NOI18N
        loginErrorMsgLabel.setFont(resourceMap.getFont("loginErrorMsgLabel.font")); // NOI18N
        loginErrorMsgLabel.setForeground(resourceMap.getColor("loginErrorMsgLabel.foreground")); // NOI18N
        loginErrorMsgLabel.setText(resourceMap.getString("loginErrorMsgLabel.text")); // NOI18N
        loginErrorMsgLabel.setName("loginErrorMsgLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout loginDialogLayout = new org.jdesktop.layout.GroupLayout(loginDialog.getContentPane());
        loginDialog.getContentPane().setLayout(loginDialogLayout);
        loginDialogLayout.setHorizontalGroup(
            loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loginDialogLayout.createSequentialGroup()
                .add(111, 111, 111)
                .add(appTitleLabel)
                .addContainerGap(596, Short.MAX_VALUE))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, loginDialogLayout.createSequentialGroup()
                .add(262, 262, 262)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel3)
                    .add(jLabel1))
                .add(18, 18, 18)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(loginUsernameTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                    .add(planetLabUserPasswordField))
                .add(286, 286, 286))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, loginDialogLayout.createSequentialGroup()
                .add(202, 202, 202)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(selectPublicKeyButton)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loginPublicKeyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 501, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loginDialogLayout.createSequentialGroup()
                        .add(loginPublicKeyPasswordField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                        .add(42, 42, 42)
                        .add(loginNoPublicKeyCheckBox)))
                .add(80, 80, 80))
            .add(loginDialogLayout.createSequentialGroup()
                .add(175, 175, 175)
                .add(jLabel2)
                .add(18, 18, 18)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loginDialogLayout.createSequentialGroup()
                        .add(loginErrorMsgLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 357, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(loginDialogLayout.createSequentialGroup()
                            .add(LoginDetailsEnterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(49, 49, 49)
                            .add(loginCancelButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap())
                        .add(loginDialogLayout.createSequentialGroup()
                            .add(sliceTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                            .add(283, 283, 283)))))
        );
        loginDialogLayout.setVerticalGroup(
            loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loginDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(appTitleLabel)
                .add(18, 18, 18)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loginUsernameTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(planetLabUserPasswordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .add(18, 18, 18)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(selectPublicKeyButton)
                    .add(loginPublicKeyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loginPublicKeyPasswordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4)
                    .add(loginNoPublicKeyCheckBox))
                .add(27, 27, 27)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sliceTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .add(30, 30, 30)
                .add(loginDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(LoginDetailsEnterButton)
                    .add(loginCancelButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 26, Short.MAX_VALUE)
                .add(loginErrorMsgLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        publicKeyDialog.setName("publicKeyDialog"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        publicKeyFileChooser.setName("publicKeyFileChooser"); // NOI18N
        publicKeyFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publicKeyFileChooserActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(publicKeyFileChooser, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(publicKeyFileChooser, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout publicKeyDialogLayout = new org.jdesktop.layout.GroupLayout(publicKeyDialog.getContentPane());
        publicKeyDialog.getContentPane().setLayout(publicKeyDialogLayout);
        publicKeyDialogLayout.setHorizontalGroup(
            publicKeyDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(publicKeyDialogLayout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        publicKeyDialogLayout.setVerticalGroup(
            publicKeyDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(publicKeyDialogLayout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        AddHostsDialog.setName("AddHostsDialog"); // NOI18N

        addHostButton.setText(resourceMap.getString("addHostButton.text")); // NOI18N
        addHostButton.setName("addHostButton"); // NOI18N
        addHostButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHostButtonActionPerformed(evt);
            }
        });

        exitAddinghostsButton.setText(resourceMap.getString("exitAddinghostsButton.text")); // NOI18N
        exitAddinghostsButton.setName("exitAddinghostsButton"); // NOI18N
        exitAddinghostsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitAddinghostsButtonActionPerformed(evt);
            }
        });

        hostnameField1.setText(resourceMap.getString("hostnameField1.text")); // NOI18N
        hostnameField1.setName("hostnameField1"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        hostAddedLabel.setText(resourceMap.getString("hostAddedLabel.text")); // NOI18N
        hostAddedLabel.setName("hostAddedLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout AddHostsDialogLayout = new org.jdesktop.layout.GroupLayout(AddHostsDialog.getContentPane());
        AddHostsDialog.getContentPane().setLayout(AddHostsDialogLayout);
        AddHostsDialogLayout.setHorizontalGroup(
            AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(AddHostsDialogLayout.createSequentialGroup()
                .add(49, 49, 49)
                .add(AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel8)
                    .add(AddHostsDialogLayout.createSequentialGroup()
                        .add(AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(hostAddedLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(hostnameField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE))
                        .add(74, 74, 74)
                        .add(AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(exitAddinghostsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
                            .add(addHostButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE))))
                .addContainerGap())
        );
        AddHostsDialogLayout.setVerticalGroup(
            AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(AddHostsDialogLayout.createSequentialGroup()
                .add(AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(AddHostsDialogLayout.createSequentialGroup()
                        .add(46, 46, 46)
                        .add(addHostButton))
                    .add(AddHostsDialogLayout.createSequentialGroup()
                        .add(21, 21, 21)
                        .add(jLabel8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(hostnameField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(AddHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(AddHostsDialogLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 39, Short.MAX_VALUE)
                        .add(exitAddinghostsButton)
                        .addContainerGap())
                    .add(AddHostsDialogLayout.createSequentialGroup()
                        .add(29, 29, 29)
                        .add(hostAddedLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        ImportHostsDialog.setName("ImportHostsDialog"); // NOI18N

        hostsFileChooser.setName("hostsFileChooser"); // NOI18N
        hostsFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostsFileChooserActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setName("jPanel3"); // NOI18N

        jLabel7.setFont(resourceMap.getFont("jLabel7.font")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        importedFileSelectedLabel.setText(resourceMap.getString("importedFileSelectedLabel.text")); // NOI18N
        importedFileSelectedLabel.setName("importedFileSelectedLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(69, 69, 69)
                        .add(jLabel7))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(139, 139, 139)
                        .add(importedFileSelectedLabel)))
                .addContainerGap(57, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(39, 39, 39)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 29, Short.MAX_VALUE)
                .add(importedFileSelectedLabel)
                .add(20, 20, 20))
        );

        importHostsButton.setText(resourceMap.getString("importHostsButton.text")); // NOI18N
        importHostsButton.setName("importHostsButton"); // NOI18N
        importHostsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importHostsButtonActionPerformed(evt);
            }
        });

        cancelImportHostsButton.setText(resourceMap.getString("cancelImportHostsButton.text")); // NOI18N
        cancelImportHostsButton.setName("cancelImportHostsButton"); // NOI18N
        cancelImportHostsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelImportHostsButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout ImportHostsDialogLayout = new org.jdesktop.layout.GroupLayout(ImportHostsDialog.getContentPane());
        ImportHostsDialog.getContentPane().setLayout(ImportHostsDialogLayout);
        ImportHostsDialogLayout.setHorizontalGroup(
            ImportHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(ImportHostsDialogLayout.createSequentialGroup()
                .add(21, 21, 21)
                .add(hostsFileChooser, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 113, Short.MAX_VALUE)
                .add(ImportHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(importHostsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(cancelImportHostsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(64, 64, 64))
            .add(ImportHostsDialogLayout.createSequentialGroup()
                .add(94, 94, 94)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(78, Short.MAX_VALUE))
        );
        ImportHostsDialogLayout.setVerticalGroup(
            ImportHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(ImportHostsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(ImportHostsDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(hostsFileChooser, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(ImportHostsDialogLayout.createSequentialGroup()
                        .add(99, 99, 99)
                        .add(importHostsButton)
                        .add(63, 63, 63)
                        .add(cancelImportHostsButton)))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jScrollPane1.setName("jScrollPane1"); // NOI18N
        jScrollPane1.setPreferredSize(new java.awt.Dimension(250, 300));

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setName("jTable1"); // NOI18N
        jScrollPane1.setViewportView(jTable1);

        loadJobDialog.setName("loadJobDialog"); // NOI18N

        LoadJobLabel.setText(resourceMap.getString("LoadJobLabel.text")); // NOI18N
        LoadJobLabel.setName("LoadJobLabel"); // NOI18N

        loadJobAsExperimentButton.setText(resourceMap.getString("loadJobAsExperimentButton.text")); // NOI18N
        loadJobAsExperimentButton.setName("loadJobAsExperimentButton"); // NOI18N

        jobsComboBox.setName("jobsComboBox"); // NOI18N

        org.jdesktop.layout.GroupLayout loadJobDialogLayout = new org.jdesktop.layout.GroupLayout(loadJobDialog.getContentPane());
        loadJobDialog.getContentPane().setLayout(loadJobDialogLayout);
        loadJobDialogLayout.setHorizontalGroup(
            loadJobDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loadJobDialogLayout.createSequentialGroup()
                .add(loadJobDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loadJobDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(LoadJobLabel))
                    .add(loadJobDialogLayout.createSequentialGroup()
                        .add(32, 32, 32)
                        .add(loadJobAsExperimentButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 214, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(28, 28, 28)
                        .add(jobsComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 334, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(77, Short.MAX_VALUE))
        );
        loadJobDialogLayout.setVerticalGroup(
            loadJobDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loadJobDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(LoadJobLabel)
                .add(88, 88, 88)
                .add(loadJobDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loadJobAsExperimentButton)
                    .add(jobsComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(78, Short.MAX_VALUE))
        );

        daemonLogDialog.setName("daemonLogDialog"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        daemonLogTextArea.setColumns(20);
        daemonLogTextArea.setRows(5);
        daemonLogTextArea.setName("daemonLogTextArea"); // NOI18N
        jScrollPane4.setViewportView(daemonLogTextArea);

        daemonLogLabel.setText(resourceMap.getString("daemonLogLabel.text")); // NOI18N
        daemonLogLabel.setName("daemonLogLabel"); // NOI18N

        exitDaemonLogsButton.setText(resourceMap.getString("exitDaemonLogsButton.text")); // NOI18N
        exitDaemonLogsButton.setName("exitDaemonLogsButton"); // NOI18N
        exitDaemonLogsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitDaemonLogsButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout daemonLogDialogLayout = new org.jdesktop.layout.GroupLayout(daemonLogDialog.getContentPane());
        daemonLogDialog.getContentPane().setLayout(daemonLogDialogLayout);
        daemonLogDialogLayout.setHorizontalGroup(
            daemonLogDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(daemonLogDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(daemonLogDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(daemonLogDialogLayout.createSequentialGroup()
                        .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 702, Short.MAX_VALUE)
                        .addContainerGap())
                    .add(daemonLogDialogLayout.createSequentialGroup()
                        .add(daemonLogLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 345, Short.MAX_VALUE)
                        .add(exitDaemonLogsButton)
                        .add(79, 79, 79))))
        );
        daemonLogDialogLayout.setVerticalGroup(
            daemonLogDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, daemonLogDialogLayout.createSequentialGroup()
                .addContainerGap()
                .add(daemonLogDialogLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(daemonLogLabel)
                    .add(exitDaemonLogsButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 7, Short.MAX_VALUE)
                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 365, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void planetLabAccountMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_planetLabAccountMenuItemActionPerformed
        loginErrorMsgLabel.setText("");
        loginBox();
    }//GEN-LAST:event_planetLabAccountMenuItemActionPerformed

    private void selectPublicKeyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectPublicKeyButtonActionPerformed
        publicKeyBox();

        if (loginUsernameTextField.getText().length() > 2) {
            LoginDetailsEnterButton.setEnabled(true);
        } else {
            loginErrorMsgLabel.setText("Please enter your username to continue.");
        }
    }//GEN-LAST:event_selectPublicKeyButtonActionPerformed

    private void loginCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginCancelButtonActionPerformed
        loginDialog.setVisible(false);
    }//GEN-LAST:event_loginCancelButtonActionPerformed

    private void LoginDetailsEnterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginDetailsEnterButtonActionPerformed
        String username = loginUsernameTextField.getText();
        String password = new String(planetLabUserPasswordField.getPassword());
        String slice = sliceTextField.getText();
        String keyPath = loginPublicKeyTextField.getText();
        boolean publicKeyNotNeeded = loginNoPublicKeyCheckBox.isEnabled();
        String publicKeyPassword = new String(loginPublicKeyPasswordField.getPassword());

        Credentials cred = null;

        if (slice.length() > 0) {
            cred = new PlanetLabCredentials(username, password, slice, keyPath, publicKeyPassword);
        } else {
            cred = new SshCredentials(username, password, keyPath, publicKeyPassword);
        }

        getSClient().setCredentials(cred);

        usernameLabel.setText(username);

        loginDialog.setVisible(false);
        loginDialog.dispose();
        getSClient().refreshNodesStatus(selectedEntries);
    }//GEN-LAST:event_LoginDetailsEnterButtonActionPerformed

    private void publicKeyFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_publicKeyFileChooserActionPerformed
        JFileChooser chooser = (JFileChooser) evt.getSource();
        if (JFileChooser.APPROVE_SELECTION.equals(evt.getActionCommand())) {
            File file = chooser.getSelectedFile();
            loginPublicKeyTextField.setText(file.getAbsolutePath());
            publicKeyDialog.setVisible(false);
        } else if (JFileChooser.CANCEL_SELECTION.equals(evt.getActionCommand())) {
            publicKeyDialog.setVisible(false);
        }

    }//GEN-LAST:event_publicKeyFileChooserActionPerformed

    private void addHostsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHostsMenuItemActionPerformed

        addHostsBox();

    }//GEN-LAST:event_addHostsMenuItemActionPerformed

    private void addHostButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHostButtonActionPerformed
        String hostname = hostnameField1.getText();
        Host h = new ExperimentHost(hostname);
        Set<Host> hosts = new HashSet<Host>();
        hosts.add(h);
        getSClient().addNodes(hosts);

        hostAddedLabel.setText("Added host: " + hostname);

        hostScrollPane.getViewport().setView(hostList);

    }//GEN-LAST:event_addHostButtonActionPerformed

    private void DefineExperimentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DefineExperimentMenuItemActionPerformed


        ExperimentWizardPanel2.setHosts(getSClient().getHosts());
        ExperimentWizardPanel4b.setHosts(getSClient().getHosts());

        Class[] pages = new Class[]{
            ExperimentWizardPanel1.class,
            ExperimentWizardPanel2.class,
            ExperimentWizardPanel3a.class,
            ExperimentWizardPanel4a.class
            , ExperimentWizardPanel4b.class
        };

        //Use the utility method to compose a Wizard
        Wizard wizard = WizardPage.createWizard(pages, WizardResultProducer.NO_OP);

//        wizard.show();
        Object results = WizardDisplayer.showWizard(wizard,
                new Rectangle(0, 50, 1300, 700));

        Map<String, Object> res = (Map<String, Object>) results;

        String artifactId = (String) res.get(ExperimentWizardPanel1.ARTIFACT_ID);
        String groupId = (String) res.get(ExperimentWizardPanel1.GROUP_ID);
        String version = (String) res.get(ExperimentWizardPanel1.VERSION);
        String repoId = (String) res.get(ExperimentWizardPanel1.REPO_ID);
        String repoUrl = (String) res.get(ExperimentWizardPanel1.REPO_URL);
        String mainClass = (String) res.get(ExperimentWizardPanel1.MAIN_CLASS);
        String args = (String) res.get(ExperimentWizardPanel1.ARGS);

        ArtifactJob experimentArtifact = new ArtifactJob(groupId, artifactId, version, repoId, repoUrl, mainClass, args,
                Integer.toString(MasterConfiguration.DEFAULT_PEER_PORT), 
                Integer.toString(MasterConfiguration.DEFAULT_WEB_PORT));

        String bootArtifactId = (String) res.get(ExperimentWizardPanel3a.ARTIFACT_ID);
        String bootGroupId = (String) res.get(ExperimentWizardPanel3a.GROUP_ID);
        String bootVersion = (String) res.get(ExperimentWizardPanel3a.VERSION);
        String bootRepoId = (String) res.get(ExperimentWizardPanel3a.REPO_ID);
        String bootRepoUrl = (String) res.get(ExperimentWizardPanel3a.REPO_URL);
        String bootMainClass = (String) res.get(ExperimentWizardPanel3a.MAIN_CLASS);
        String bootArgs = (String) res.get(ExperimentWizardPanel3a.ARGS);
        String bootPort = (String) res.get(ExperimentWizardPanel3a.PORT);
        String bootWebPort = (String) res.get(ExperimentWizardPanel3a.WEB_PORT);

        ArtifactJob bootArtifact = new ArtifactJob(bootGroupId, bootArtifactId, 
                bootVersion, bootRepoId, bootRepoUrl, bootMainClass, bootArgs, bootPort, bootWebPort);

        String monitorArtifactId = (String) res.get(ExperimentWizardPanel4a.ARTIFACT_ID);
        String monitorGroupId = (String) res.get(ExperimentWizardPanel4a.GROUP_ID);
        String monitorVersion = (String) res.get(ExperimentWizardPanel4a.VERSION);
        String monitorRepoId = (String) res.get(ExperimentWizardPanel4a.REPO_ID);
        String monitorRepoUrl = (String) res.get(ExperimentWizardPanel4a.REPO_URL);
        String monitorMainClass = (String) res.get(ExperimentWizardPanel4a.MAIN_CLASS);
        String monitorArgs = (String) res.get(ExperimentWizardPanel4a.ARGS);
        String monitorPort = (String) res.get(ExperimentWizardPanel4a.PORT);
        String monitorWebPort = (String) res.get(ExperimentWizardPanel4a.WEB_PORT);

        ArtifactJob monitorArtifact = new ArtifactJob(monitorGroupId, monitorArtifactId, 
                monitorVersion, monitorRepoId, monitorRepoUrl, monitorMainClass, monitorArgs,
                monitorPort, monitorWebPort);


        String numNodes = (String) res.get("numHostsLabel");
        int numHosts = 0;
        if (numNodes != null) {
            numHosts = Integer.parseInt(numNodes);
        }

        String hostnames[] = extractHostnames("nodesTA", res);
        String monitor[] = extractHostnames("monitor", res);
        String bootstrap[] = extractHostnames("bootstrap", res);


        ExpPane expPane = new ExpPane(getSClient(),
                experimentArtifact, hostnames,
                bootArtifact, bootstrap[0], bootPort, bootWebPort,
                monitorArtifact, monitor[0], monitorPort, monitorWebPort,
                experimentCounter);

        int jobId = PomUtils.generateJobId(groupId, artifactId, version);

        getSClient().registerExperiment(jobId, expPane);
        mainTabbedPane.addTab("Experiment " + experimentCounter, expPane);
        experimentCounter++;


        mainTabbedPane.requestFocusInWindow();
        expPane.requestFocus();

    }//GEN-LAST:event_DefineExperimentMenuItemActionPerformed

    private String[] extractHostnames(String componentName, Map<String, Object> res)
    {
        String expHosts = (String) res.get(componentName);

        String[] nodes = expHosts.split(ExperimentWizardPanel2.NEWLINE);

        String[] hostnames = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            String node = nodes[i];
            String[] params = node.split(":");
            hostnames[i] = params[0];
        }

        return hostnames;
    }

    private void DefineExperimentMenuItemMenuKeyPressed(javax.swing.event.MenuKeyEvent evt) {//GEN-FIRST:event_DefineExperimentMenuItemMenuKeyPressed
    }//GEN-LAST:event_DefineExperimentMenuItemMenuKeyPressed

    private void DefineExperimentMenuItemMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DefineExperimentMenuItemMouseClicked
    }//GEN-LAST:event_DefineExperimentMenuItemMouseClicked

    private void BackgroundTaskMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BackgroundTaskMenuItemActionPerformed
        new SayHelloTask(getApplication());
    }//GEN-LAST:event_BackgroundTaskMenuItemActionPerformed

    private void hostsFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hostsFileChooserActionPerformed
    }//GEN-LAST:event_hostsFileChooserActionPerformed

    private void importHostsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importHostsButtonActionPerformed

        File hostsCsvFile = hostsFileChooser.getSelectedFile();
        try {
            Set<Host> hosts = HostsParser.parseHostsFile(hostsCsvFile);
            getSClient().addNodes(hosts);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        } catch (HostsParserException ex) {
            Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_importHostsButtonActionPerformed

    private void exitAddinghostsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitAddinghostsButtonActionPerformed
        AddHostsDialog.setVisible(false);
        AddHostsDialog.dispose();
    }//GEN-LAST:event_exitAddinghostsButtonActionPerformed

    private void importHostsMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importHostsMIActionPerformed
        importHostsBox();
    }//GEN-LAST:event_importHostsMIActionPerformed

    private void cancelImportHostsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelImportHostsButtonActionPerformed
        ImportHostsDialog.setVisible(false);
        ImportHostsDialog.dispose();
    }//GEN-LAST:event_cancelImportHostsButtonActionPerformed

    private void addHostButtonMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHostButtonMainActionPerformed
        addHostsBox();
    }//GEN-LAST:event_addHostButtonMainActionPerformed

    private void removeHostButtonMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeHostButtonMainActionPerformed
        getSClient().removeNodes(selectedEntries);
    }//GEN-LAST:event_removeHostButtonMainActionPerformed

    private void startDaemonButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startDaemonButtonActionPerformed
        getSClient().startDaemon(selectedEntries);
    }//GEN-LAST:event_startDaemonButtonActionPerformed

    private void installDaemonButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installDaemonButtonActionPerformed
        getSClient().installDaemonOnNodes(selectedEntries, forceDaemonInstallCheckBox.isSelected());
    }//GEN-LAST:event_installDaemonButtonActionPerformed

    private void installJavaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installJavaButtonActionPerformed
        getSClient().installJava(selectedEntries, forceJavaInstallCheckBox.isSelected());
    }//GEN-LAST:event_installJavaButtonActionPerformed

    private void loginNoPublicKeyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginNoPublicKeyCheckBoxActionPerformed

        if (loginUsernameTextField.getText().length() > 2) {
            LoginDetailsEnterButton.setEnabled(true);
        }
    }//GEN-LAST:event_loginNoPublicKeyCheckBoxActionPerformed

    private void updateHostsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateHostsButtonActionPerformed
        getSClient().refreshNodesStatus(selectedEntries);
    }//GEN-LAST:event_updateHostsButtonActionPerformed

    private void stopDaemonButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopDaemonButtonActionPerformed
        getSClient().stopDaemon(selectedEntries);
    }//GEN-LAST:event_stopDaemonButtonActionPerformed

    private void getDaemonLogsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getDaemonLogsButtonActionPerformed
        getSClient().daemonLogs(selectedEntries);
    }//GEN-LAST:event_getDaemonLogsButtonActionPerformed

    private void viewDaemonLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewDaemonLogButtonActionPerformed
         if (selectedEntries.size() == 1) {
            Iterator<NodeEntry> iter = selectedEntries.iterator();
            NodeEntry node = iter.next();
            String host = node.getHostname();
            File f = new File (Configuration.DAEMON_LOGS_DIR + host + "/daemon.log");
            if (f.exists() == true) {

                String contents = getContents(f);
                daemonLogTextArea.setText(contents);
                DesktopApplication1.getApplication().show(daemonLogDialog);
            }
            else {
                nodeEntryMsgL.setText("To view, first download daemon logs for this host.");
                viewDaemonLogButton.setEnabled(false);
            }
        }
    }//GEN-LAST:event_viewDaemonLogButtonActionPerformed


    static public String getContents(File aFile) {
    //...checks on aFile are elided
    StringBuilder contents = new StringBuilder();

    try {
      //use buffering, reading one line at a time
      //FileReader always assumes default encoding is OK!
      BufferedReader input =  new BufferedReader(new FileReader(aFile));
      try {
        String line = null; //not declared within while loop
        /*
        * readLine is a bit quirky :
        * it returns the content of a line MINUS the newline.
        * it returns null only for the END of the stream.
        * it returns an empty String if two newlines appear in a row.
        */
        while (( line = input.readLine()) != null){
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
      }
      finally {
        input.close();
      }
    }
    catch (IOException ex){
      ex.printStackTrace();
      return "Error when reading the file: " + aFile.getAbsolutePath();
    }

    return contents.toString();
  }


    private void loadExperimentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExperimentMenuItemActionPerformed

        // TODO load the jobs from client
        List<Job> jobs = getSClient().getJobs();
        String[] jobNames = new String[jobs.size()];
        int i=0;
        for (Job job : jobs) {
            jobNames[i] = job.getGroupId() + ":" + job.getArtifactId() + ":" + job.getVersion();
            i++;
        }

        ComboBoxModel model = new DefaultComboBoxModel(jobNames);

        jobsComboBox.setModel(model);

        DesktopApplication1.getApplication().show(loadJobDialog);
    }//GEN-LAST:event_loadExperimentMenuItemActionPerformed

    private void exitDaemonLogsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitDaemonLogsButtonActionPerformed
         daemonLogDialog.setVisible(false);
         daemonLogDialog.dispose();
    }//GEN-LAST:event_exitDaemonLogsButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog AddHostsDialog;
    private javax.swing.JMenuItem BackgroundTaskMenuItem;
    private javax.swing.JMenuItem DefineExperimentMenuItem;
    private javax.swing.JDialog ImportHostsDialog;
    private javax.swing.JLabel LoadJobLabel;
    private javax.swing.JButton LoginDetailsEnterButton;
    private javax.swing.JMenu accountMenu;
    private javax.swing.JPopupMenu accountPopupMenu;
    private javax.swing.JButton addHostButton;
    private javax.swing.JButton addHostButtonMain;
    private javax.swing.JMenuItem addHostsMenuItem;
    private javax.swing.JButton cancelImportHostsButton;
    private javax.swing.JDialog daemonLogDialog;
    private javax.swing.JLabel daemonLogLabel;
    private javax.swing.JTextArea daemonLogTextArea;
    private javax.swing.JButton exitAddinghostsButton;
    private javax.swing.JButton exitDaemonLogsButton;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JCheckBox forceDaemonInstallCheckBox;
    private javax.swing.JCheckBox forceJavaInstallCheckBox;
    private javax.swing.JButton getDaemonLogsButton;
    private javax.swing.JLabel hostAddedLabel;
    private javax.swing.JList hostList;
    private javax.swing.JScrollPane hostScrollPane;
    private javax.swing.JTextField hostnameField1;
    private javax.swing.JFileChooser hostsFileChooser;
    private javax.swing.JButton importHostsButton;
    private javax.swing.JMenuItem importHostsMI;
    private javax.swing.JLabel importedFileSelectedLabel;
    private javax.swing.JButton installDaemonButton;
    private javax.swing.JButton installJavaButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable jTable1;
    private javax.swing.JComboBox jobsComboBox;
    private javax.swing.JMenuItem loadExperimentMenuItem;
    private javax.swing.JButton loadJobAsExperimentButton;
    private javax.swing.JDialog loadJobDialog;
    private javax.swing.JButton loginCancelButton;
    private javax.swing.JDialog loginDialog;
    private javax.swing.JLabel loginErrorMsgLabel;
    private javax.swing.JCheckBox loginNoPublicKeyCheckBox;
    private javax.swing.JPasswordField loginPublicKeyPasswordField;
    private javax.swing.JTextField loginPublicKeyTextField;
    private javax.swing.JTextField loginUsernameTextField;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel mainTabPanel;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel nodeEntryMsgL;
    private javax.swing.JMenuItem planetLabAccountMenuItem;
    private javax.swing.JPasswordField planetLabUserPasswordField;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JDialog publicKeyDialog;
    private javax.swing.JFileChooser publicKeyFileChooser;
    private javax.swing.JButton removeHostButtonMain;
    private javax.swing.JButton selectPublicKeyButton;
    private javax.swing.JTextField sliceTextField;
    private javax.swing.JLabel sshKeyfileLabel;
    private javax.swing.JButton startDaemonButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JButton stopDaemonButton;
    private javax.swing.JButton updateHostsButton;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JButton viewDaemonLogButton;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;

    @Override
    public void intervalAdded(ListDataEvent arg0) {
//        throw new UnsupportedOperationException("Not supported yet.");
        System.out.println("Added a host");
    }

    @Override
    public void intervalRemoved(ListDataEvent arg0) {
//        throw new UnsupportedOperationException("Not supported yet.");
        System.out.println("Removed a host");
    }

    @Override
    public void contentsChanged(ListDataEvent arg0) {
//        throw new UnsupportedOperationException("Not supported yet.");
        System.out.println("List contents changed event received by DesktopView");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getSource() instanceof UserEntry) {
            entryChanged((UserEntry) evt.getSource(), evt.getPropertyName(),
                    evt.getOldValue());
        }
    }

    private void entryChanged(UserEntry userEntry,
            String propertyChanged, Object lastValue) {

        if (propertyChanged == UserEntry.USER) {
            usernameLabel.setText(userEntry.getSshLoginName());
        }
        if (propertyChanged == UserEntry.KEYFILE_PATH) {
            sshKeyfileLabel.setText(userEntry.getSshKeyFilename());
        }
    }

    public class SayHelloTask extends Task<Void, Void> {

        public SayHelloTask(Application app) {
            super(app);
        }

        @Override
        protected Void doInBackground() {
            for (int i = 0; i <= 10; i++) {
//                progress(i, 0, 10); // calls setProgress()
                message("hello", i); // resource defines format
                try {
                    Thread.currentThread().sleep(150L);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            return null;
        }

        @Override
        protected void succeeded(Void result) {
            message("done");
        }

        @Override
        protected void cancelled() {
            message("cancelled");

        }

        @Override
        protected void failed(Throwable cause) {
            message("failed");
        }

        @Override
        protected void interrupted(InterruptedException e) {
        }
    }

    @Action
    public Task sayHello() { // Say hello repeatedly
        return new SayHelloTask(getApplication());
    }

    @Action
    public void loadMap() throws IOException {
        String file = ((DesktopApplication1) getApplication()).getSessionFile();
        Object map = getApplication().getContext().getLocalStorage().load(file);
//    ListModel listModel.setMap((LinkedHashMap<String, String>)map);
//    showFileMessage("loadedFile", file);
    }

    @Action
    public void saveMap() throws IOException {
        String file = ((DesktopApplication1) getApplication()).getSessionFile();
//    LinkedHashMap<String, String> map = listModel.getMap();
        LinkedHashMap<String, String> map = null;
        getApplication().getContext().getLocalStorage().save(map, file);
//    showFileMessage("savedFile", file);
    }

    private final class TabbedPaneChangeHandler implements ChangeListener {

        private final JTabbedPane tp;
        private int selectedIndex;

        TabbedPaneChangeHandler(JTabbedPane tp) {
            this.tp = tp;
        }

        public void stateChanged(ChangeEvent e) {
            if (tp.getSelectedIndex() == 1 && tp.getComponentAt(1) == null) {
//                tp.setComponentAt(1, createNotesPanel());
            }
            selectedIndex = tp.getSelectedIndex();
        }
    }

    private void disableControls() {
//        undoManager.setIgnoreEdits(true);
//        hostTF.setEditable(false);
//        hostTF.setText("");
//        userTF.setEditable(false);
//        userTF.setText("");
//        passwordTF.setEditable(false);
//        passwordTF.setText("");
//        if (notesTP != null) {
//            notesTP.setText("");
//            notesTP.setEditable(false);
//        }
//        imagePanel.setImage(null);
//        imagePanel.setEditable(false);
//        imagePanel.setBackground(passwordTF.getBackground());
//        visualizer.setPassword(null);
//        undoManager.setIgnoreEdits(false);
        }

    private void enableControls() {
    }

    private void listSelectionChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        viewDaemonLogButton.setEnabled(false);
        
        Object[] vals = hostList.getSelectedValues();
        selectedEntries.clear();
        for (Object v : vals) {
            selectedEntries.add((NodeEntry) v);
            System.out.println("Selected : " + ((NodeEntry) v).getHostname());
        }


        if (selectedEntries.size() == 1) {
            Iterator<NodeEntry> iter = selectedEntries.iterator();
            NodeEntry node = iter.next();
            nodeEntryMsgL.setText(node.getMsg());
            if (new File(Configuration.DAEMON_LOGS_DIR + node.getHostname() + "/daemon.log").exists())
            {
                logger.info("Found daemon log file for " + node.getHostname());
                viewDaemonLogButton.setEnabled(true);
            }
            else {
                logger.info("Couldn't find logs for " + Configuration.DAEMON_LOGS_DIR + node.getHostname()
                        + "/daemon.log" );
            }
        }
    }
}
