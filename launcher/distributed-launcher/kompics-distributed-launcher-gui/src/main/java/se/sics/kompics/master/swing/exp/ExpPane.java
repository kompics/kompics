/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExpPane.java
 *
 * Created on 18-Aug-2009, 15:27:12
 */
package se.sics.kompics.master.swing.exp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import se.sics.kompics.wan.job.ArtifactJob;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import se.sics.kompics.address.Address;
import se.sics.kompics.master.swing.Client;
import se.sics.kompics.master.swing.DesktopApplication1;
import se.sics.kompics.master.swing.model.ExecEntry;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;

/**
 *
 * @author jdowling
 */
public class ExpPane extends javax.swing.JPanel implements TreeModelListener, TreeSelectionListener, PropertyChangeListener {

    private static final String MONITOR_EDITOR = "Edit Monitor Server Details";
    private static final String BOOTSTRAP_EDITOR = "Edit Bootstrap Server Details";
    private static final String EXPERIMENT_EDITOR = "Edit Experiment Details";
    private static final Logger logger = Logger.getLogger(ExpPane.class.getCanonicalName());
    private final Client client;
    private final int expId;
    protected DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Hosts");
    private DefaultMutableTreeNode selectedNode = null;
    private ArtifactJob monitorArtifact;
    private ArtifactJob bootstrapArtifact;
    private ArtifactJob experimentArtifact;

    /** Creates new form ExpPane */
    public ExpPane(Client client,
            ArtifactJob experiment, String[] hosts,
            ArtifactJob bootstrapArtifact,
            String bootstrapHost, String bootstrapPort, String bootstrapWebPort,
            ArtifactJob monitorArtifact,
            String monitorHost, String monitorPort, String monitorWebPort,
            int expId) {
        initComponents();
        this.client = client;
        this.monitorArtifact = monitorArtifact;
        this.bootstrapArtifact = bootstrapArtifact;
        this.experimentArtifact = experiment;

        this.expId = expId;
        this.expLabel.setText("Experiment " + expId);

        client.setBootstrapServer(bootstrapHost);
        client.setMonitorServer(monitorHost);

        ExpTreeModel treeModel = new ExpTreeModel(rootNode);
        tree.setModel(treeModel);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
//        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.putClientProperty("JTree.lineStyle", "Angled");

        tree.addTreeSelectionListener(this);

        client.putTreeModel(this.expId, treeModel);

        for (String host : hosts) {
            addObject(new ExpEntry(host,
                    ExpEntry.ExperimentStatus.NOT_LOADED, true));
        }

    }

    public DefaultMutableTreeNode addObject(ExpEntry child) {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;
        } else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }


        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
            ExpEntry child) {
        return addObject(parent, child, false);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
            ExpEntry child,
            boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        //It is key to invoke this on the TreeModel, and NOT DefaultMutableTreeNode
        client.getTreeModel(this.expId).insertNodeInto(childNode, parent,
                parent.getChildCount());

        child.addPropertyChangeListener(client.getTreeModel(this.expId));

        //Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    public void createChildren(ExpEntry parent, List<Address> peers) {

        for (Address addr : peers) {
            addObject(new ExpEntry(addr.toString(),
                    parent.getStatus(), false));
        }

    }

    public ExpEntry getNodeContents(DefaultMutableTreeNode node) {
        return (ExpEntry) node.getUserObject();
    }

    /** Remove the currently selected node. */
    public void removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                client.getTreeModel(expId).removeNodeFromParent(currentNode);
                return;
            }
        }

        logger.log(Level.WARNING, "Either there was no selection or something else");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        artifactEditorDialog = new javax.swing.JDialog();
        jPanel4 = new javax.swing.JPanel();
        args = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        mainClass = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        groupId = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        artifactId = new javax.swing.JTextField();
        version = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        repoId = new javax.swing.JTextField();
        repoUrl = new javax.swing.JTextField();
        saveArtifactButton = new javax.swing.JButton();
        cancelSaveArtifact = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        artifactPort = new javax.swing.JTextField();
        artifactWebPort = new javax.swing.JTextField();
        editorLabel = new javax.swing.JLabel();
        loadExperimentButton = new javax.swing.JButton();
        startExperimentButton = new javax.swing.JButton();
        stopExperimentButton = new javax.swing.JButton();
        collectLogsButton = new javax.swing.JButton();
        startMonitorButton = new javax.swing.JButton();
        startBootstrapButton = new javax.swing.JButton();
        expLabel = new javax.swing.JLabel();
        hideMavenOutputCheckBox = new javax.swing.JCheckBox();
        addPeerButton = new javax.swing.JButton();
        removePeersButton = new javax.swing.JButton();
        treeScrollPane = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        numPeersComboBox = new javax.swing.JComboBox();
        editMonitorButton = new javax.swing.JButton();
        editBootstrapButton = new javax.swing.JButton();
        editExperimentButton = new javax.swing.JButton();
        errorMsgLabel = new javax.swing.JLabel();
        stopBootstrapButton = new javax.swing.JButton();
        stopMonitorButton = new javax.swing.JButton();

        artifactEditorDialog.setName("artifactEditorDialog"); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel4.setName("jPanel4"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(se.sics.kompics.master.swing.DesktopApplication1.class).getContext().getResourceMap(ExpPane.class);
        args.setFont(resourceMap.getFont("args.font")); // NOI18N
        args.setName("args"); // NOI18N

        jLabel20.setText(resourceMap.getString("jLabel20.text")); // NOI18N
        jLabel20.setName("jLabel20"); // NOI18N

        mainClass.setFont(resourceMap.getFont("mainClass.font")); // NOI18N
        mainClass.setName("mainClass"); // NOI18N

        jLabel21.setText(resourceMap.getString("jLabel21.text")); // NOI18N
        jLabel21.setName("jLabel21"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(args, javax.swing.GroupLayout.DEFAULT_SIZE, 857, Short.MAX_VALUE)
                    .addComponent(jLabel21)
                    .addComponent(jLabel20)
                    .addComponent(mainClass, javax.swing.GroupLayout.PREFERRED_SIZE, 565, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(args, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel5.setName("jPanel5"); // NOI18N

        jLabel22.setText(resourceMap.getString("jLabel22.text")); // NOI18N
        jLabel22.setName("jLabel22"); // NOI18N

        groupId.setFont(resourceMap.getFont("groupId.font")); // NOI18N
        groupId.setName("groupId"); // NOI18N

        jLabel18.setText(resourceMap.getString("jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        artifactId.setFont(resourceMap.getFont("artifactId.font")); // NOI18N
        artifactId.setName("artifactId"); // NOI18N

        version.setFont(resourceMap.getFont("version.font")); // NOI18N
        version.setName("version"); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(groupId, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(artifactId, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22)
                    .addComponent(jLabel18)
                    .addComponent(version, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel5Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {artifactId, groupId, version});

        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(artifactId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(version, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel5Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {artifactId, groupId, version});

        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel6.setName("jPanel6"); // NOI18N

        jLabel19.setText(resourceMap.getString("jLabel19.text")); // NOI18N
        jLabel19.setName("jLabel19"); // NOI18N

        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        repoId.setFont(resourceMap.getFont("repoId.font")); // NOI18N
        repoId.setName("repoId"); // NOI18N

        repoUrl.setFont(resourceMap.getFont("repoUrl.font")); // NOI18N
        repoUrl.setName("repoUrl"); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(repoUrl, javax.swing.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel19)
                            .addComponent(repoId, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(171, 171, 171))))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(repoId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(repoUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        saveArtifactButton.setText(resourceMap.getString("saveArtifactButton.text")); // NOI18N
        saveArtifactButton.setName("saveArtifactButton"); // NOI18N
        saveArtifactButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveArtifactButtonActionPerformed(evt);
            }
        });

        cancelSaveArtifact.setText(resourceMap.getString("cancelSaveArtifact.text")); // NOI18N
        cancelSaveArtifact.setName("cancelSaveArtifact"); // NOI18N
        cancelSaveArtifact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelSaveArtifactActionPerformed(evt);
            }
        });

        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel7.setName("jPanel7"); // NOI18N

        jLabel23.setText(resourceMap.getString("jLabel23.text")); // NOI18N
        jLabel23.setName("jLabel23"); // NOI18N

        jLabel24.setText(resourceMap.getString("jLabel24.text")); // NOI18N
        jLabel24.setName("jLabel24"); // NOI18N

        artifactPort.setFont(resourceMap.getFont("artifactPort.font")); // NOI18N
        artifactPort.setName("artifactPort"); // NOI18N

        artifactWebPort.setFont(resourceMap.getFont("artifactWebPort.font")); // NOI18N
        artifactWebPort.setName("artifactWebPort"); // NOI18N

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel24)
                    .addComponent(artifactPort, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(49, 49, 49)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel23)
                    .addComponent(artifactWebPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(80, Short.MAX_VALUE))
        );

        jPanel7Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {artifactPort, artifactWebPort});

        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(artifactPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(artifactWebPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        editorLabel.setText(resourceMap.getString("editorLabel.text")); // NOI18N
        editorLabel.setName("editorLabel"); // NOI18N

        javax.swing.GroupLayout artifactEditorDialogLayout = new javax.swing.GroupLayout(artifactEditorDialog.getContentPane());
        artifactEditorDialog.getContentPane().setLayout(artifactEditorDialogLayout);
        artifactEditorDialogLayout.setHorizontalGroup(
            artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(artifactEditorDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, artifactEditorDialogLayout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cancelSaveArtifact, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(saveArtifactButton, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(28, 28, 28))
            .addGroup(artifactEditorDialogLayout.createSequentialGroup()
                .addGap(172, 172, 172)
                .addComponent(editorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 379, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(372, Short.MAX_VALUE))
        );
        artifactEditorDialogLayout.setVerticalGroup(
            artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, artifactEditorDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(editorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(artifactEditorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(artifactEditorDialogLayout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(artifactEditorDialogLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(saveArtifactButton)
                        .addGap(28, 28, 28)
                        .addComponent(cancelSaveArtifact)
                        .addGap(88, 88, 88))
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        setName("ExpForm"); // NOI18N
        setPreferredSize(new java.awt.Dimension(793, 765));

        loadExperimentButton.setText(resourceMap.getString("loadExperimentButton.text")); // NOI18N
        loadExperimentButton.setName("loadExperimentButton"); // NOI18N
        loadExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadExperimentButtonActionPerformed(evt);
            }
        });

        startExperimentButton.setText(resourceMap.getString("startExperimentButton.text")); // NOI18N
        startExperimentButton.setName("startExperimentButton"); // NOI18N
        startExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startExperimentButtonActionPerformed(evt);
            }
        });

        stopExperimentButton.setText(resourceMap.getString("stopExperimentButton.text")); // NOI18N
        stopExperimentButton.setName("stopExperimentButton"); // NOI18N
        stopExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopExperimentButtonActionPerformed(evt);
            }
        });

        collectLogsButton.setText(resourceMap.getString("collectLogsButton.text")); // NOI18N
        collectLogsButton.setEnabled(false);
        collectLogsButton.setName("collectLogsButton"); // NOI18N
        collectLogsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                collectLogsButtonActionPerformed(evt);
            }
        });

        startMonitorButton.setText(resourceMap.getString("startMonitorButton.text")); // NOI18N
        startMonitorButton.setName("startMonitorButton"); // NOI18N
        startMonitorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startMonitorButtonActionPerformed(evt);
            }
        });

        startBootstrapButton.setText(resourceMap.getString("startBootstrapButton.text")); // NOI18N
        startBootstrapButton.setName("startBootstrapButton"); // NOI18N
        startBootstrapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBootstrapButtonActionPerformed(evt);
            }
        });

        expLabel.setFont(resourceMap.getFont("expLabel.font")); // NOI18N
        expLabel.setText(resourceMap.getString("expLabel.text")); // NOI18N
        expLabel.setName("expLabel"); // NOI18N

        hideMavenOutputCheckBox.setSelected(true);
        hideMavenOutputCheckBox.setText(resourceMap.getString("hideMavenOutputCheckBox.text")); // NOI18N
        hideMavenOutputCheckBox.setName("hideMavenOutputCheckBox"); // NOI18N
        hideMavenOutputCheckBox.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                hideMavenOutputCheckBoxPropertyChange(evt);
            }
        });

        addPeerButton.setText(resourceMap.getString("Add Peers.text")); // NOI18N
        addPeerButton.setName("Add Peers"); // NOI18N
        addPeerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPeerButtonActionPerformed(evt);
            }
        });

        removePeersButton.setText(resourceMap.getString("removePeersButton.text")); // NOI18N
        removePeersButton.setName("removePeersButton"); // NOI18N
        removePeersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePeersButtonActionPerformed(evt);
            }
        });

        treeScrollPane.setName("treeScrollPane"); // NOI18N

        tree.setName("tree"); // NOI18N
        treeScrollPane.setViewportView(tree);

        numPeersComboBox.setBackground(resourceMap.getColor("numPeersComboBox.background")); // NOI18N
        numPeersComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        numPeersComboBox.setName("numPeersComboBox"); // NOI18N

        editMonitorButton.setText(resourceMap.getString("editMonitorButton.text")); // NOI18N
        editMonitorButton.setName("editMonitorButton"); // NOI18N
        editMonitorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editMonitorButtonActionPerformed(evt);
            }
        });

        editBootstrapButton.setText(resourceMap.getString("editBootstrapButton.text")); // NOI18N
        editBootstrapButton.setName("editBootstrapButton"); // NOI18N
        editBootstrapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editBootstrapButtonActionPerformed(evt);
            }
        });

        editExperimentButton.setText(resourceMap.getString("editExperimentButton.text")); // NOI18N
        editExperimentButton.setName("editExperimentButton"); // NOI18N
        editExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editExperimentButtonActionPerformed(evt);
            }
        });

        errorMsgLabel.setFont(resourceMap.getFont("errorMsgLabel.font")); // NOI18N
        errorMsgLabel.setText(resourceMap.getString("errorMsgLabel.text")); // NOI18N
        errorMsgLabel.setName("errorMsgLabel"); // NOI18N

        stopBootstrapButton.setText(resourceMap.getString("stopBootstrapButton.text")); // NOI18N
        stopBootstrapButton.setName("stopBootstrapButton"); // NOI18N
        stopBootstrapButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBootstrapButtonActionPerformed(evt);
            }
        });

        stopMonitorButton.setText(resourceMap.getString("stopMonitorButton.text")); // NOI18N
        stopMonitorButton.setName("stopMonitorButton"); // NOI18N
        stopMonitorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopMonitorButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(editExperimentButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(78, 78, 78))
                    .addComponent(collectLogsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addComponent(hideMavenOutputCheckBox, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadExperimentButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startExperimentButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addComponent(stopExperimentButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(removePeersButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                            .addComponent(addPeerButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                            .addComponent(expLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(numPeersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(treeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                    .addComponent(errorMsgLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(startBootstrapButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(startMonitorButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(stopBootstrapButton, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(stopMonitorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(editMonitorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(editBootstrapButton)))
                .addGap(84, 84, 84))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addPeerButton, collectLogsButton, editExperimentButton, loadExperimentButton, removePeersButton, startExperimentButton, stopExperimentButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {startBootstrapButton, startMonitorButton, stopBootstrapButton, stopMonitorButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(startBootstrapButton)
                            .addComponent(startMonitorButton)
                            .addComponent(stopBootstrapButton)
                            .addComponent(stopMonitorButton)
                            .addComponent(editMonitorButton)
                            .addComponent(editBootstrapButton))
                        .addGap(21, 21, 21)
                        .addComponent(errorMsgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(expLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(addPeerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numPeersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removePeersButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hideMavenOutputCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadExperimentButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startExperimentButton)
                        .addGap(18, 18, 18)
                        .addComponent(stopExperimentButton)
                        .addGap(18, 18, 18)
                        .addComponent(collectLogsButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editExperimentButton))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(treeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 473, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(195, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {addPeerButton, collectLogsButton, editExperimentButton, loadExperimentButton, removePeersButton, startBootstrapButton, startExperimentButton, startMonitorButton, stopBootstrapButton, stopExperimentButton, stopMonitorButton});

    }// </editor-fold>//GEN-END:initComponents

    private Set<Host> getHosts() {

        ExpEntry selectedEntry = (ExpEntry) selectedNode.getUserObject();

        LinkedHashSet<Host> hosts = new LinkedHashSet<Host>();

        Address addr = selectedEntry.getAddress();
        if (addr != null) {
            Host h = new ExperimentHost(addr.getIp().getHostAddress());  // , addr.getPort()
            hosts.add(h);
        }

        return hosts;
//        LinkedHashSet<ExpEntry> entries = client.getTreeModel(expId).get;
//        LinkedHashSet<Host> hosts = new LinkedHashSet<Host>();
//        for (ExpEntry e : entries) {
//            hosts.add(e.getHost());
//        }
//        return hosts;
    }

    private void loadExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExperimentButtonActionPerformed

        boolean hideOutput = hideMavenOutputCheckBox.isSelected();
        String[] args = experimentArtifact.getArgs().split(" ");
        List<String> listArgs = Arrays.asList(args);
        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
        String hostname = exp.getAddress().getIp().getHostAddress();

        Set<Host> hosts = new HashSet<Host>();
        hosts.add(new ExperimentHost(hostname));

        ExpEntry selectedEntry = (ExpEntry) selectedNode.getUserObject();
        if (selectedEntry.isDaemon() == false) {
            errorMsgLabel.setText("Error: can only load an experiment at a daemon (not a peer or at root)");
        } else {
            errorMsgLabel.setText("Loading an experiment at " + selectedEntry.getAddress());
            client.installJobOnHosts(experimentArtifact.getGroupId(), experimentArtifact.getArtifactId(),
                    experimentArtifact.getVersion(),
                    experimentArtifact.getMainClass(),
                    listArgs,
                    experimentArtifact.getRepoId(), experimentArtifact.getRepoUrl(),
                    hideOutput,
                    hosts);
        }
    }//GEN-LAST:event_loadExperimentButtonActionPerformed

    private int getNumberPeers() {
        return selectedNode.getChildCount();
    }

    private void startExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startExperimentButtonActionPerformed

        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
        String hostname = exp.getAddress().getIp().getHostAddress();

        experimentArtifact.setPort(Integer.toString(exp.getAddress().getPort()));

        ExecEntry execEntry = new ExecEntry();

        if (exp.isDaemon() == true) {
            int numPeers = selectedNode.getChildCount();
            errorMsgLabel.setText("Starting experiment on " + exp.getAddress() + " with " + numPeers + " peers");
            client.startJob(hostname, experimentArtifact, bootstrapArtifact, monitorArtifact, numPeers,
                    execEntry);
        } else {
            
            errorMsgLabel.setText("Starting experiment  on " + exp.getAddress() );
            client.startJob(hostname, experimentArtifact, bootstrapArtifact, monitorArtifact, 1, execEntry);
            errorMsgLabel.setText("Cannot start bootstrap on a peer. Select a daemon node, then click start.");
        }
        
    }//GEN-LAST:event_startExperimentButtonActionPerformed

    private void stopExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopExperimentButtonActionPerformed
       ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
        String hostname = exp.getAddress().getIp().getHostAddress();

        errorMsgLabel.setText("Stopping experiment at " + exp.getAddress().toString());

        client.stopJob(hostname, experimentArtifact.getGroupId(), experimentArtifact.getArtifactId(), experimentArtifact.getVersion());
    }//GEN-LAST:event_stopExperimentButtonActionPerformed

    private void startBootstrapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startBootstrapButtonActionPerformed

        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
//        String hostname = exp.getAddress().getIp().getHostAddress();

        ExecEntry execEntry = new ExecEntry();

        if (exp.isDaemon() == true) {
            errorMsgLabel.setText("Starting bootstrap server on " + client.getBootstrapServer()
                    + ":" + bootstrapArtifact.getWebPort());
            client.startBootstrap(bootstrapArtifact, execEntry);
        } else {
            errorMsgLabel.setText("Cannot start bootstrap on a peer. Select a daemon node, then click start.");
        }
    }//GEN-LAST:event_startBootstrapButtonActionPerformed

    private void startMonitorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startMonitorButtonActionPerformed

        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
//        String hostname = exp.getAddress().getIp().getHostAddress();

        ExecEntry execEntry = new ExecEntry();
        if (exp.isDaemon() == true) {
            errorMsgLabel.setText("Starting monitor server on " + client.getMonitorServer()
                    + ":" + monitorArtifact.getWebPort());
            client.startMonitor(monitorArtifact, execEntry);
        } else {
            errorMsgLabel.setText("Cannot start monitor on a peer. Select a daemon node, then click start.");
        }

    }//GEN-LAST:event_startMonitorButtonActionPerformed

    private void collectLogsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_collectLogsButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_collectLogsButtonActionPerformed

    private void addPeerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPeerButtonActionPerformed
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            Object userObj = currentNode.getUserObject();

            if (userObj instanceof ExpEntry && userObj != null) {
                int numChildren = currentNode.getChildCount();
                ExpEntry nodeEntry = (ExpEntry) userObj;
                Address addr = nodeEntry.getAddress();
                if (addr == null) {
                    errorMsgLabel.setText("Address for selection was null.");
                    return;
                }
                String host = addr.toString();
                String[] parts = host.split("@"); // throw away the peer-id,
                String hostPort = parts[1]; // keep host-port#

                String n = (String) numPeersComboBox.getSelectedItem();
                if (n != null) {
                    int numPeers = Integer.parseInt(n);
                    List<Address> peers = new ArrayList<Address>();
                    for (int i = numChildren + 2; i < numPeers + numChildren + 2; i++) {
                        Address peer = new Address(addr.getIp(), addr.getPort(), i);
                        peers.add(peer);
                    }
                    createChildren(nodeEntry, peers);
                }
                errorMsgLabel.setText("Added peers to a top-level node.");
            } else {
                errorMsgLabel.setText("You can only add peers to a top-level node (but not root).");
            }
        }
    }//GEN-LAST:event_addPeerButtonActionPerformed

    private void removePeersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePeersButtonActionPerformed
        removeCurrentNode();
    }//GEN-LAST:event_removePeersButtonActionPerformed

    private void hideMavenOutputCheckBoxPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_hideMavenOutputCheckBoxPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_hideMavenOutputCheckBoxPropertyChange

    private void saveArtifactButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveArtifactButtonActionPerformed
        if (editorLabel.getText().compareTo(MONITOR_EDITOR) == 0) {
            monitorArtifact = new ArtifactJob(groupId.getText(), artifactId.getText(), version.getText(),
                    repoId.getText(), repoUrl.getText(), mainClass.getText(), args.getText(),
                    artifactPort.getText(), artifactWebPort.getText());
        } else if (editorLabel.getText().compareTo(BOOTSTRAP_EDITOR) == 0) {
            bootstrapArtifact = new ArtifactJob(groupId.getText(), artifactId.getText(), version.getText(),
                    repoId.getText(), repoUrl.getText(), mainClass.getText(), args.getText(),
                    artifactPort.getText(), artifactWebPort.getText());
        } else if (editorLabel.getText().compareTo(EXPERIMENT_EDITOR) == 0) {
            experimentArtifact = new ArtifactJob(groupId.getText(), artifactId.getText(), version.getText(),
                    repoId.getText(), repoUrl.getText(), mainClass.getText(), args.getText(),
                    artifactPort.getText(), artifactWebPort.getText());
        }
        artifactEditorDialog.setVisible(false);
        artifactEditorDialog.dispose();
    }//GEN-LAST:event_saveArtifactButtonActionPerformed

    private void cancelSaveArtifactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelSaveArtifactActionPerformed
        artifactEditorDialog.setVisible(false);
        artifactEditorDialog.dispose();
    }//GEN-LAST:event_cancelSaveArtifactActionPerformed

    private void editMonitorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editMonitorButtonActionPerformed
        groupId.setText(monitorArtifact.getGroupId());
        artifactId.setText(monitorArtifact.getArtifactId());
        version.setText(monitorArtifact.getVersion());
        repoId.setText(monitorArtifact.getRepoId());
        repoUrl.setText(monitorArtifact.getRepoUrl());
        mainClass.setText(monitorArtifact.getMainClass());
        args.setText(monitorArtifact.getArgs());
        artifactPort.setText(monitorArtifact.getPort());
        artifactWebPort.setText(monitorArtifact.getWebPort());
        editorLabel.setText(MONITOR_EDITOR);
        DesktopApplication1.getApplication().show(artifactEditorDialog);
    }//GEN-LAST:event_editMonitorButtonActionPerformed

    private void editBootstrapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editBootstrapButtonActionPerformed
        groupId.setText(bootstrapArtifact.getGroupId());
        artifactId.setText(bootstrapArtifact.getArtifactId());
        version.setText(bootstrapArtifact.getVersion());
        repoId.setText(bootstrapArtifact.getRepoId());
        repoUrl.setText(bootstrapArtifact.getRepoUrl());
        mainClass.setText(bootstrapArtifact.getMainClass());
        args.setText(bootstrapArtifact.getArgs());
        artifactPort.setText(bootstrapArtifact.getPort());
        artifactWebPort.setText(bootstrapArtifact.getWebPort());
        editorLabel.setText(BOOTSTRAP_EDITOR);

        DesktopApplication1.getApplication().show(artifactEditorDialog);
    }//GEN-LAST:event_editBootstrapButtonActionPerformed

    private void editExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editExperimentButtonActionPerformed
        groupId.setText(experimentArtifact.getGroupId());
        artifactId.setText(experimentArtifact.getArtifactId());
        version.setText(experimentArtifact.getVersion());
        repoId.setText(experimentArtifact.getRepoId());
        repoUrl.setText(experimentArtifact.getRepoUrl());
        mainClass.setText(experimentArtifact.getMainClass());
        args.setText(experimentArtifact.getArgs());
        artifactPort.setText(experimentArtifact.getPort());
        artifactWebPort.setText(experimentArtifact.getWebPort());
//        artifactPort.setEditable(false);
//        artifactWebPort.setEditable(false);
        editorLabel.setText(EXPERIMENT_EDITOR);
        DesktopApplication1.getApplication().show(artifactEditorDialog);
    }//GEN-LAST:event_editExperimentButtonActionPerformed

    private void stopBootstrapButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopBootstrapButtonActionPerformed

        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();

        errorMsgLabel.setText("Stopping bootstrap server at " + exp.getAddress().toString());

        client.stopJob(client.getBootstrapServer(), bootstrapArtifact.getGroupId(), bootstrapArtifact.getArtifactId(), bootstrapArtifact.getVersion());
    }//GEN-LAST:event_stopBootstrapButtonActionPerformed

    private void stopMonitorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopMonitorButtonActionPerformed
         ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
        String hostname = exp.getAddress().getIp().getHostAddress();
        errorMsgLabel.setText("Stopping monitor server at " + exp.getAddress().toString());
        client.stopJob(client.getMonitorServer(), monitorArtifact.getGroupId(), monitorArtifact.getArtifactId(), monitorArtifact.getVersion());
    }//GEN-LAST:event_stopMonitorButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPeerButton;
    private javax.swing.JTextField args;
    private javax.swing.JDialog artifactEditorDialog;
    private javax.swing.JTextField artifactId;
    private javax.swing.JTextField artifactPort;
    private javax.swing.JTextField artifactWebPort;
    private javax.swing.JButton cancelSaveArtifact;
    private javax.swing.JButton collectLogsButton;
    private javax.swing.JButton editBootstrapButton;
    private javax.swing.JButton editExperimentButton;
    private javax.swing.JButton editMonitorButton;
    private javax.swing.JLabel editorLabel;
    private javax.swing.JLabel errorMsgLabel;
    private javax.swing.JLabel expLabel;
    private javax.swing.JTextField groupId;
    private javax.swing.JCheckBox hideMavenOutputCheckBox;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JButton loadExperimentButton;
    private javax.swing.JTextField mainClass;
    private javax.swing.JComboBox numPeersComboBox;
    private javax.swing.JButton removePeersButton;
    private javax.swing.JTextField repoId;
    private javax.swing.JTextField repoUrl;
    private javax.swing.JButton saveArtifactButton;
    private javax.swing.JButton startBootstrapButton;
    private javax.swing.JButton startExperimentButton;
    private javax.swing.JButton startMonitorButton;
    private javax.swing.JButton stopBootstrapButton;
    private javax.swing.JButton stopExperimentButton;
    private javax.swing.JButton stopMonitorButton;
    private javax.swing.JTree tree;
    private javax.swing.JScrollPane treeScrollPane;
    private javax.swing.JTextField version;
    // End of variables declaration//GEN-END:variables

    public void treeNodesChanged(TreeModelEvent e) {
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

        /*
         * If the event lists children, then the changed
         * node is the child of the node we've already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */

        int index = e.getChildIndices()[0];
        node = (DefaultMutableTreeNode) (node.getChildAt(index));

        System.out.println("The user has finished editing the node.");
        System.out.println("New value: " + node.getUserObject());
    }

    public void treeNodesInserted(TreeModelEvent e) {
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    }

    public void treeStructureChanged(TreeModelEvent e) {
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {

        if (e.getSource() instanceof DefaultMutableTreeNode) {
            selectedNode = (DefaultMutableTreeNode) e.getSource();
        } else if (e.getSource() instanceof JTree) {
            JTree tree = (JTree) e.getSource();
            TreePath[] paths = tree.getSelectionPaths();
            if (paths != null) {
                if (paths.length == 1) {
                    logger.info("Selected node was changed.");
                    selectedNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
                }
            }
        }
//        ExpEntry exp = (ExpEntry) selectedNode.getUserObject();
//        logger.info("Selected node is now " + exp.getAddress().toString());
    }

    public void updateNodeState(InetAddress ip, int jobId, ExpEntry.ExperimentStatus status) {
        // TODO: need to synchronize on the root node here
        Enumeration children = rootNode.children();
        ;
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();

            ExpEntry exp = (ExpEntry) node.getUserObject();

            if (exp.getAddress().getIp().equals(ip)) {
                exp.setStatus(status);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {

        String val = (String) arg0.getNewValue();
        errorMsgLabel.setText(val);

    }
}
