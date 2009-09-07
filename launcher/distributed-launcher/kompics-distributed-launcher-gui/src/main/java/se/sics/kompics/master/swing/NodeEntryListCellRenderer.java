/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package se.sics.kompics.master.swing;

import java.awt.Color;
import java.awt.Component;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.LineBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.master.swing.model.NodeEntry;
import se.sics.kompics.master.swing.model.NodeEntry.ConnectionStatus;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus.Installation;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus.Program;

/**
 * ListCellRenderer for the PasswordEntrys. 
 *
 * @author sky
 */
final class NodeEntryListCellRenderer extends JPanel implements ListCellRenderer {

    private static final Logger logger = LoggerFactory.getLogger(NodeEntryListCellRenderer.class);

    private static final int LIST_CELL_ICON_SIZE = 36;
    private JLabel hostLabel;
    private JCheckBox javaCheckBox;
    private JCheckBox daemonCheckBox;
    private JCheckBox daemonRunningCheckBox;
    private JCheckBox sshConnectionCheckBox;
    private VerticalSessionConnectedBar connectedBar;

    NodeEntryListCellRenderer() {
        hostLabel = new JLabel(" ");
        javaCheckBox = new JCheckBox("Java Installed", false);
        daemonCheckBox = new JCheckBox("Daemon Installed", false);
        daemonRunningCheckBox = new JCheckBox("Daemon Running", false);
        sshConnectionCheckBox = new JCheckBox("Ssh", false);

        connectedBar = new VerticalSessionConnectedBar();
        connectedBar.setOpaque(true);
        connectedBar.setBackground(Color.WHITE);
        connectedBar.setBorder(new LineBorder(Color.BLACK));

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        GroupLayout.SequentialGroup hg = layout.createSequentialGroup();
        layout.setHorizontalGroup(hg);
        hg.addGroup(layout.createParallelGroup().
                addComponent(hostLabel, 10, 10, Integer.MAX_VALUE));
        hg.addGroup(layout.createParallelGroup().
                addComponent(javaCheckBox, 10, 10, Integer.MAX_VALUE));
        hg.addGroup(layout.createParallelGroup().
                addComponent(daemonCheckBox, 10, 10, Integer.MAX_VALUE));
        hg.addGroup(layout.createParallelGroup().
                addComponent(sshConnectionCheckBox, 10, 10, Integer.MAX_VALUE));
        hg.addGroup(layout.createParallelGroup().
                addComponent(daemonRunningCheckBox, 10, 10, Integer.MAX_VALUE));
        hg.addGroup(layout.createParallelGroup().
                addComponent(connectedBar, 18, 18, 18));

        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.addGroup(layout.createSequentialGroup().
                addComponent(hostLabel));
        vg.addGroup(layout.createSequentialGroup().
                addComponent(javaCheckBox));
        vg.addGroup(layout.createSequentialGroup().
                addComponent(daemonCheckBox));
        vg.addGroup(layout.createSequentialGroup().
                addComponent(sshConnectionCheckBox));
        vg.addGroup(layout.createSequentialGroup().
                addComponent(daemonRunningCheckBox));
        vg.addGroup(layout.createSequentialGroup().
                addComponent(connectedBar));

        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
                NodeEntry entry = (NodeEntry) value;
        String host = entry.getHostname();
        if (host == null) {
            host = " ";
        }

        hostLabel.setText(host);

        if (entry.getJavaInstallStatus() == Installation.INSTALLED) {
            javaCheckBox.setSelected(true);
        }
        else if (entry.getJavaInstallStatus() == Installation.NOT_INSTALLED) {
            javaCheckBox.setSelected(false);
        }
        else {
            javaCheckBox.setSelected(false);
        }


        if (entry.getDaemonInstallStatus() == Installation.INSTALLED) {
            daemonCheckBox.setSelected(true);
        }
        else if (entry.getDaemonInstallStatus() == Installation.NOT_INSTALLED) {
            daemonCheckBox.setSelected(false);
        }
        else {
            daemonCheckBox.setSelected(false);
        }

        if (entry.getDaemonRunningStatus() == Program.RUNNING) {
            daemonRunningCheckBox.setSelected(true);
        }
        else if (entry.getDaemonRunningStatus() == Program.STOPPED) {
            daemonRunningCheckBox.setSelected(false);
        }

        ConnectionStatus sshStatus = entry.getSshConnectionStatus();
        if (sshStatus == ConnectionStatus.CONNECTED)
        {
            sshConnectionCheckBox.setSelected(true);
        }
        else {
            sshConnectionCheckBox.setSelected(false);
        }

        ConnectionStatus daemonConnectionStatus = entry.getDaemonConnectionStatus();
        float strength=0.5f;
        if (daemonConnectionStatus == ConnectionStatus.CONNECTED)
        {
            if (entry.getDaemonRunningStatus() != Program.RUNNING) {
                strength=0.5f;
            }
            else {
                strength = 1.0f;
            }
        }
        else if (daemonConnectionStatus == ConnectionStatus.NOT_CONTACTABLE)
        {
            strength=0.0f;
        }
        else if (daemonConnectionStatus == ConnectionStatus.NOT_CONNECTED)
        {
            strength=0.3f;
            if (sshStatus == ConnectionStatus.CONNECTED) {
                strength=0.4f;
            }
            if (entry.getDaemonRunningStatus() == Program.RUNNING) {
                strength=0.5f;
            }
        }




//        int strength = (sessionId == 0) ? 0 : 100;
        if (entry != null) {
            connectedBar.setStrength(strength);
        }
        if (isSelected) {
            adjustColors(list.getSelectionBackground(),
                    list.getSelectionForeground(), this, hostLabel, javaCheckBox,
                    daemonCheckBox, daemonRunningCheckBox, connectedBar);
        } else {
            adjustColors(list.getBackground(),
                    list.getForeground(), this, hostLabel, javaCheckBox,
                    daemonCheckBox, daemonRunningCheckBox, connectedBar);
        }
        return this;
    }

    private void adjustColors(Color bg, Color fg, Component... components) {
        for (Component c : components) {
            c.setForeground(fg);
            c.setBackground(bg);
        }
    }
}
