package se.sics.kompics.master.swing.model;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus.Installation;
import se.sics.kompics.wan.services.ExperimentServicesComponent.ServicesStatus.Program;
import se.sics.kompics.wan.ssh.Host;

public class NodeEntry extends AbstractEntry implements Serializable, Comparable<NodeEntry> {

    private static final long serialVersionUID = 59743112233441L;

    public static final String HOSTNAME = "hostname";
    public static final String SSH_CONNECTION_STATUS = "sshConnection";
    public static final String DAEMON_INSTALL_STATUS = "daemonInstall";
    public static final String DAEMON_RUNNING_STATUS = "daemonRunning";
    public static final String DAEMON_CONNECTION_STATUS = "daemonConnected";
    public static final String JAVA_STATUS = "java installed?";

    public enum ConnectionStatus {

        NOT_CONNECTED, CONNECTED, NOT_CONTACTABLE
    }

    private Host host;
    private ServicesStatus.Installation daemonInstallStatus = Installation.NOT_INSTALLED;
    private ServicesStatus.Installation javaInstallStatus = Installation.NOT_INSTALLED;
    private ServicesStatus.Program daemonRunningStatus = Program.STOPPED;
    private ConnectionStatus sshConnectionStatus = ConnectionStatus.NOT_CONNECTED;
    private ConnectionStatus daemonConnectionStatus = ConnectionStatus.NOT_CONNECTED;

    private String msg="";

    public NodeEntry() {
    }

    public NodeEntry(Host h) {
        host = h;
    }

    public void setHostname(String hostname) {
        String oldHost = this.host.getHostname();
        this.host.setHostname(hostname);
        firePropertyChange(NodeEntry.HOSTNAME, oldHost, hostname);
    }

    public String getHostname() {
        return host.getHostname();
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        String oldHost = this.host.getHostname();
        this.host = host;
        firePropertyChange(NodeEntry.HOSTNAME, oldHost, this.host.getHostname());
        // XXX firePropertyChange for all fields here
    }
    
    public int getSessionId()
    {
        return host.getSessionId();
    }
    public void setSessionId(int sessionId) {
        int oldId = host.getSessionId();
        this.host.setSessionId(sessionId);
        firePropertyChange(SSH_CONNECTION_STATUS, oldId, sessionId);
    }

    public ServicesStatus.Installation getDaemonInstallStatus() {
        return daemonInstallStatus;
    }

    public Program getDaemonRunningStatus() {
        return daemonRunningStatus;
    }

    public ConnectionStatus getSshConnectionStatus() {
        return sshConnectionStatus;
    }

    public ConnectionStatus getDaemonConnectionStatus() {
        return daemonConnectionStatus;
    }

    public ServicesStatus.Installation getJavaInstallStatus() {
        return javaInstallStatus;
    }

    public void setJavaInstallStatus(ServicesStatus.Installation javaStatus) {
        ServicesStatus.Installation oldStatus = this.javaInstallStatus;
        this.javaInstallStatus = javaStatus;
        firePropertyChange(NodeEntry.JAVA_STATUS, oldStatus, this.javaInstallStatus);
    }

    public void setDaemonInstallStatus(ServicesStatus.Installation daemonStatus) {
        ServicesStatus.Installation oldStatus = this.daemonInstallStatus;
        this.daemonInstallStatus = daemonStatus;
        firePropertyChange(NodeEntry.DAEMON_INSTALL_STATUS, oldStatus, this.daemonInstallStatus);
    }

    public void setDaemonRunningStatus(ServicesStatus.Program daemonStatus) {
        ServicesStatus.Program oldStatus = this.daemonRunningStatus;
        this.daemonRunningStatus = daemonStatus;
        firePropertyChange(NodeEntry.DAEMON_RUNNING_STATUS, oldStatus, this.daemonRunningStatus);
    }

    public void setConnectionStatus(ConnectionStatus sshConnectionStatus) {
        ConnectionStatus oldStatus = this.sshConnectionStatus;
        this.sshConnectionStatus = sshConnectionStatus;
        firePropertyChange(NodeEntry.SSH_CONNECTION_STATUS, oldStatus, this.sshConnectionStatus);
    }

    public void setDaemonConnectionStatus(ConnectionStatus daemonConnectionStatus) {
        ConnectionStatus oldStatus = this.daemonConnectionStatus;
        this.daemonConnectionStatus = daemonConnectionStatus;
        firePropertyChange(NodeEntry.DAEMON_CONNECTION_STATUS, oldStatus, this.daemonConnectionStatus);
    }

    public NodeEntry clone() {
        NodeEntry entry = new NodeEntry();
        try {
            entry.host = (Host) host.clone();
            entry.daemonInstallStatus = this.daemonInstallStatus;
            entry.sshConnectionStatus = this.sshConnectionStatus;
            entry.javaInstallStatus = javaInstallStatus;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(NodeEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        return entry;
    }

    @Override
    public int hashCode() {
        return host.hashCode();
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == this) {
            return true;
        }
        if (arg0 instanceof NodeEntry == false) {
            return false;
        }
        NodeEntry that = (NodeEntry) arg0;
        return this.host.equals(that.host);
    }

    @Override
    public int compareTo(NodeEntry that) {
        return this.host.compareTo(that.host);
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
