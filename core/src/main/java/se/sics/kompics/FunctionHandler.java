/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics;

import java.util.function.Consumer;

/**
 *
 * @author lkroll
 */
public class FunctionHandler<E extends KompicsEvent> extends Handler<E> {
    
    private final Consumer<E> eventHandler;
    
    public FunctionHandler(Class<E> eventType, Consumer<E> eventHandler) {
        super(eventType);
        this.eventHandler = eventHandler;
    }

    @Override
    public void handle(E event) {
        eventHandler.accept(event);
    }
    
}
