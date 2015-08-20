/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.test;

import se.sics.kompics.Component;
import se.sics.kompics.network.Address;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public interface NetworkGenerator {
    public Component generate(ComponentProxy parent, Address self);
}
