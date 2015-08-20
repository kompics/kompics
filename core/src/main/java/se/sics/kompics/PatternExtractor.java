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
public interface PatternExtractor<P, V> extends KompicsEvent {
    public P extractPattern();
    public V extractValue();
}
