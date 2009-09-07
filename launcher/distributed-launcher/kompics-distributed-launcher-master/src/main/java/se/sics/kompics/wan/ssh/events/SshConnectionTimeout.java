/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.wan.ssh.events;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 *
 * @author jdowling
 */
public class SshConnectionTimeout extends Timeout {

    public static final int SSH_CONNECT_TIMEOUT = 120 * 1000;
    public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;
    private final String host;

    public SshConnectionTimeout(ScheduleTimeout request, String host) {
        super(request);
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    
}
