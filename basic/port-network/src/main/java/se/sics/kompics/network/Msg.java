/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.network;

import se.sics.kompics.KompicsEvent;

/**
 *
 * @author lkroll
 */
public interface Msg<Adr extends Address, H extends Header<Adr>> extends KompicsEvent {
    public H getHeader();
    /**
     * Fields forwarded from Header for backwards compatibility
     * 
     * @return
     * @deprecated
     */
    @Deprecated
    public Adr getSource();
    @Deprecated
    public Adr getDestination();
    @Deprecated
    public Transport getProtocol();
}
