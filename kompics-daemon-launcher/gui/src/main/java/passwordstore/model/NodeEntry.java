/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;

import passwordstore.beansx.BeanBase;
import se.sics.kompics.wan.ssh.Host;

/**
 * HostEntry represents an account on a particular system. Each
 * HostEntry has a host name, user name, password, notes, path to
 * image and dates corresponding to last modified and accessed. All setters
 * follow the beans pattern of notifying property change listeners of changes.
 *
 * @version $Revision$
 */
public class NodeEntry extends BeanBase implements Host, Serializable {
    
	private static final long serialVersionUID = -6341540793343799943L;
	
    public static enum DaemonStatus {INSTALLED_NOT_RUNNING, INSTALLED_RUNNING, NOT_INSTALLED};
	
	private String host;
	private InetAddress ip;
	
	private String user;
    private String password;
    
    private String keyFile;
    private String keyFilePassword;
    
    private boolean connected;
     
    private int sessionId;
    
    private DaemonStatus  daemonStatus;
    
    private String notes;
    private long lastAccessed;
    private long lastModified;
    private URI imagePath;
    
    public void setHost(String host) {
        String oldHost = this.host;
        this.host = host;
        firePropertyChange("host", oldHost, host);
    }
    
    public String getHost() {
        return host;
    }
    
    public void setUser(String user) {
        String oldUser = this.user;
        this.user = user;
        firePropertyChange("user", oldUser, user);
    }
    
    public String getUser() {
        return user;
    }
    
    public void setPassword(String password) {
        String oldPassword = this.password;
        this.password = password;
        firePropertyChange("password", oldPassword, password);
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setLastAccessed(long lastAccessed) {
        long oldLastAccessed = this.lastAccessed;
        this.lastAccessed = lastAccessed;
        firePropertyChange("lastAccessed", oldLastAccessed, lastAccessed);
    }
    
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastModified(long lastModified) {
        long oldLastModified = this.lastModified;
        this.lastModified = lastModified;
        firePropertyChange("lastModified", oldLastModified, lastModified);
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void setNotes(String notes) {
        String oldNotes = this.notes;
        this.notes = notes;
        firePropertyChange("notes", oldNotes, notes);
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setImagePath(URI imagePath) {
        URI oldImagePath = this.imagePath;
        this.imagePath = imagePath;
        firePropertyChange("imagePath", oldImagePath, imagePath);
    }
    
    public URI getImagePath() {
        return imagePath;
    }
    
    public NodeEntry clone() {
        NodeEntry entry = new NodeEntry();
        entry.host = host;
        entry.user = user;
        entry.password = password;
        entry.notes = notes;
        entry.lastAccessed = lastAccessed;
        entry.lastModified = lastModified;
        entry.imagePath = imagePath;
        return entry;
    }
    
    public String getKeyFile() {
		return keyFile;
	}
    
    public void setKeyFile(String keyFile) {
    	String oldkeyFile = this.keyFile;
		this.keyFile = keyFile;
		firePropertyChange("keyFile", oldkeyFile, keyFile);		
	}
    
    public String getKeyFilePassword() {
		return keyFilePassword;
	}
    public void setKeyFilePassword(String keyFilePassword) {
    	String oldkeyFilePassword   = this.keyFilePassword ;
		this.keyFilePassword  = keyFilePassword ;
		firePropertyChange("keyFilePassword ", oldkeyFilePassword , keyFilePassword);		
	}
 
    public boolean isConnected() {
		return connected;
	}
    
    public DaemonStatus getDaemonStatus() {
		return daemonStatus;
	}

    public void setConnected(boolean connected) {
    	boolean oldConnected = this.connected;
		this.connected = connected;
		firePropertyChange("connected", oldConnected, this.connected);
	}
    
    public void setDaemonStatus(DaemonStatus daemonStatus) {
    	DaemonStatus oldStatus = this.daemonStatus;
		this.daemonStatus = daemonStatus;
		firePropertyChange("daemonStatus", oldStatus, this.daemonStatus);
		
	}

	@Override
	public int compareTo(Host host) {
		return this.getHostname().compareTo(host.getHostname());
	}

	@Override
	public String getConnectFailedPolicy() {
		return null;
	}

	@Override
	public String getHostname() {
		return getHost();
	}

	@Override
	public InetAddress getIp() {
//		InetAddress addr=null;
//		try {
//			addr = InetAddress.getByName(host);
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
//		return addr;
		return ip;
	}

	@Override
	public int getSessionId() {
		return sessionId;
	}

	@Override
	public void setConnectFailedPolicy(String connectFailedPolicy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHostname(String hostname) {
		setHost(hostname);
	}

	@Override
	public void setIp(InetAddress ip) {
		InetAddress oldIp = this.ip;
		this.ip = ip;
		firePropertyChange("ip", oldIp, this.ip);
	}

	@Override
	public void setSessionId(int sessionId) {
		int oldSessionId = this.sessionId;
		this.sessionId = sessionId;
		firePropertyChange("sessionId", oldSessionId, this.sessionId);
		
	}
}
