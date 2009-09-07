package se.sics.kompics.wan.ssh;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.kompics.wan.plab.PLabHost;

public class ExperimentHost implements Host, Serializable {

    private static final long serialVersionUID = 5950394888856113421L;
    protected String hostname = null;
    protected InetAddress ip = null;
    protected int sessionId = 0;
    protected String connectFailedPolicy = "";

    public ExperimentHost() {
    }

    /**
     * Called using results from Planetlab's XML-RPC API
     *
     * @param nodeInfo
     */
    @SuppressWarnings("unchecked")
    public ExperimentHost(Map nodeInfo) {
        hostname = (String) nodeInfo.get(PLabHost.HOSTNAME);
    }

    public ExperimentHost(String hostname) {
        this.hostname = hostname;
    }

    public ExperimentHost(int sessionId, String hostname) {
        this.sessionId = sessionId;
        this.hostname = hostname;
    }

    public ExperimentHost(int sessionId, String hostname, InetAddress ip) {
        this.sessionId = sessionId;
        this.hostname = hostname;
        this.ip = ip;
    }

    public ExperimentHost(Host host) {
        this.sessionId = host.getSessionId();
        this.hostname = host.getHostname();
        this.ip = host.getIp();
    }


    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#compareTo(se.sics.kompics.wan.ssh.ExperimentHost)
     */
    @Override
    public int compareTo(Host host) {
        if (this.ip == null) {
            try {
                this.ip = InetAddress.getByName(this.hostname);
            } catch (UnknownHostException ex) {
                Logger.getLogger(ExperimentHost.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }
        }
        InetAddress thatIp = host.getIp();
        if (thatIp == null) {
            try {
                thatIp = InetAddress.getByName(host.getHostname());
            } catch (UnknownHostException ex) {
                Logger.getLogger(ExperimentHost.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }
        }

        if (this.ip.equals(thatIp) == true) {
            return 0;
        }
        return -1;
//		return this.getHostname().compareTo(host.getHostname());
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#getConnectFailedPolicy()
     */
    public String getConnectFailedPolicy() {
        return connectFailedPolicy;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#getHostname()
     */
    public String getHostname() {
        return hostname;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#getIp()
     */
    public InetAddress getIp() {
        return ip;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#getNodeId()
     */
    public int hashCode() {
        int hash = 7;
        // XXX only supporting one connection per host, here
//			hash = 31 * hash + sessionId;
        hash = hash * ((hostname == null) ? 1 : hostname.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Host == false) {
            return false;
        }
        Host that = (Host) obj;
        
        if (this.ip == null) {
            try {
                ip = InetAddress.getByName(this.hostname);
            } catch (UnknownHostException ex) {
                Logger.getLogger(ExperimentHost.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        InetAddress thatIp = that.getIp();
        if (thatIp == null) {
            try {
                thatIp = InetAddress.getByName(that.getHostname());
            } catch (UnknownHostException ex) {
                Logger.getLogger(ExperimentHost.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        String h1 = this.ip.getCanonicalHostName();
        String h2 = thatIp.getCanonicalHostName();

        if (h1.compareToIgnoreCase(h2) != 0)
        {
            return false;
        }
        return true;
//        if (this.hostname.compareToIgnoreCase(that.getHostname()) != 0) {
//            return false;
//        }
//        return true;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#setConnectFailedPolicy(java.lang.String)
     */
    public void setConnectFailedPolicy(String connectFailedPolicy) {
        this.connectFailedPolicy = connectFailedPolicy;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#setHostname(java.lang.String)
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#setIp(java.lang.String)
     */
    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(hostname);
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#getSessionId()
     */
    public int getSessionId() {
        return sessionId;
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.wan.ssh.Host#setSessionId(int)
     */
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ExperimentHost copy = new ExperimentHost(this.sessionId, this.hostname, this.ip);
        copy.connectFailedPolicy = this.connectFailedPolicy;
        return copy;
    }
}
