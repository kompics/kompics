/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly.test;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.ClearToSend;
import se.sics.kompics.network.RequestToSend;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class TestCTS extends ClearToSend {
    public TestCTS(Address dest, Address src, RequestToSend rts) {
        super(dest, src, rts);
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
}
