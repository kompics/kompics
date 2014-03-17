/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;


/**
 *
 * @author jdowling
 */
public class MsgTimeout extends Timeout {

    public MsgTimeout(ScheduleTimeout request) {
        super(request);
    }
}
