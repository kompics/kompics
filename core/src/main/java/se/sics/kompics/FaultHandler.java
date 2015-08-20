/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import se.sics.kompics.Fault.ResolveAction;

/**
 *
 * @author lkroll
 */
public interface FaultHandler {
    public ResolveAction handle(Fault f);
}
