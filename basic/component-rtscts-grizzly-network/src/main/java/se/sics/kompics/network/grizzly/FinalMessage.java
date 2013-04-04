/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class FinalMessage extends Message {
    
    public final int requestId;
    public final int flowId;
    
    public FinalMessage(Address from, Address to, int reqId, int flowId) {
        super(from, to);
        
        this.requestId = reqId;
        this.flowId = flowId;
    }
}
