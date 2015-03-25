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
public abstract class ClassMatchedHandler<V, E extends KompicsEvent & PatternExtractor<Class<V>, V>> extends MatchedHandler<Class<V>, V, E> {
    
    
    private Class<V> matchType = null;
    
    
    public Class<V> pattern() {
        return this.matchType;
    }
    
    void setPattern(Class<V> matchType) {
        this.matchType = matchType;
    }
}
