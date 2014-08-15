/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics;

/**
 *
 * @author lkroll
 */
public class Killed implements KompicsEvent {
    public final Component component;
    
    protected Killed(Component c) {
        component = c;
    }
}
