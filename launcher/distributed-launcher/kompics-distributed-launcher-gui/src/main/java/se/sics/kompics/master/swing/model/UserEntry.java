package se.sics.kompics.master.swing.model;

import java.io.Serializable;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.SshCredentials;

public class UserEntry extends AbstractEntry implements Serializable {

    private static final long serialVersionUID = 5974374756113421L;

    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String KEYFILE_PATH = "keyfile";
    public static final String KEYFILE_PASSWORD = "keyfilePassword";
    public static final String SLICE = "slice";

    private String sshLoginName;

    private String sshKeyFilename;

    private String sshKeyFilePassword;

    public void setSshKeyFilePassword(String sshKeyFilePassword) {
        String old = this.sshKeyFilePassword;
        this.sshKeyFilePassword = sshKeyFilePassword;
        firePropertyChange(UserEntry.KEYFILE_PASSWORD, old, sshKeyFilePassword);
    }

    private String sshPassword;

    private String slice;

    public void setSshLoginName(String sshLoginName) {
        String oldUser = this.sshLoginName;
        this.sshLoginName = sshLoginName;
        firePropertyChange(UserEntry.USER, oldUser, sshLoginName);
    }
    
    public String getSshLoginName() {
        return sshLoginName;
    }
    
    public void setSshPassword(String password) {
        String oldPassword = this.sshPassword;
        this.sshPassword = password;
        firePropertyChange(UserEntry.PASSWORD, oldPassword, password);
    }
    
    public String getSshPassword() {
        return sshPassword;
    }
    
        
    public void setSlice(String notes) {
        String oldNotes = this.slice;
        this.slice = notes;
        firePropertyChange(UserEntry.SLICE, oldNotes, notes);
    }
    
    public String getSlice() {
        return slice;
    }
    
   
    public UserEntry clone() {
        UserEntry entry = new UserEntry();
        entry.sshLoginName = sshLoginName;
        entry.sshPassword = sshPassword;
        entry.slice = slice;
        entry.sshKeyFilename = sshKeyFilename;
        entry.sshKeyFilePassword = sshKeyFilePassword;
        return entry;
    }

    public void setSshKeyFilename(String sshKeyFilename) {
        String oldKeyFile = this.sshKeyFilename;
        this.sshKeyFilename = sshKeyFilename;
        firePropertyChange(UserEntry.KEYFILE_PATH, oldKeyFile, sshKeyFilename);
    }

    public String getSshKeyFilePassword() {
        return sshKeyFilePassword;
    }

    public String getSshKeyFilename() {
        return sshKeyFilename;
    }

    public Credentials getCredentials()
    {
       Credentials cred = null;
       if (this.sshLoginName != null)
       {
           cred = new SshCredentials(this.sshLoginName, this.sshPassword, this.sshKeyFilename, this.sshKeyFilePassword);
       }
       return cred;
    }
}
