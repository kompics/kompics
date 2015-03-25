/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

/**
 *
 * @author lkroll
 * @param <Matched>
 * @param <E>
 */
public abstract class MatchedHandler<P, V, E extends KompicsEvent & PatternExtractor<P, V>> {

    Class<E> cxtType = null;

    public abstract P pattern();

    public abstract void handle(V content, E context);

    /**
     * Sets the event type.
     * <p>
     * @param eventType
     * the new event type
     */
    public void setCxtType(Class<E> cxtType) {
        this.cxtType = cxtType;
    }

    /**
     * Gets the event type.
     * <p>
     * @return the event type
     */
    public Class<E> getCxtType() {
        return cxtType;
    }
}
