/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.network;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.address.Address;

/**
 *
 * @author lkroll
 */
public interface Msg extends KompicsEvent {
    public Address getSource();
    public Address getDestination();
    public Address getOrigin();
    public Transport getProtocol();
}
