package se.sics.kompics.master.swing.exp;

import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.kompics.address.Address;
import se.sics.kompics.master.swing.model.AbstractEntry;
import se.sics.kompics.wan.util.AddressParser;

/**
 * This is not a thread safe class.
 * A good convention to adopt is synchronizing on the root node of a tree.
 * @author jdowling
 */

public class ExpEntry extends AbstractEntry implements Comparable<ExpEntry>  {

    public static enum ExperimentStatus {

        NOT_LOADED, LOADED, RUNNING, FINISHED_LOGS_NOT_COLLECTED, ERROR
    };

    private String addr;

    private final boolean daemon;

    private ExperimentStatus status;

    public ExpEntry(String addr, ExperimentStatus status, boolean daemon) {
        this.addr = addr;
        this.status = status;
        this.daemon = daemon;

    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public Address getAddress()
    {
        try {
            return AddressParser.parseAddress(addr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ExpEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void setStatus(ExperimentStatus status) {

        ExperimentStatus oldStatus = this.status;
        this.status = status;
        firePropertyChange(ExpEntry.ExperimentStatus.class.getCanonicalName(), oldStatus, this.status);
    }

    public boolean isDaemon() {
        return daemon;
    }

    
    public ExpEntry clone() {
        ExpEntry entry = new ExpEntry(this.addr, this.status, this.daemon);
        return entry;
    }

    @Override
    public int hashCode() {
        return this.addr.hashCode() + this.status.hashCode() + (daemon == true ? 0 : 1);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == this) {
            return true;
        }
        if (arg0 instanceof ExpEntry == false) {
            return false;
        }

        ExpEntry that = (ExpEntry) arg0;
        if (this.addr.equals(that.addr) == true) {
            if (this.status == that.status) {
                if (this.daemon == that.daemon) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(ExpEntry that) {
        if (this.addr.equals(that.addr) == true) {
            if (this.status == that.status) {
                return 0;
            }
        }
        return 1;
    }

    @Override
    public String toString() {
        return this.addr + "--" + this.status;
    }

}
