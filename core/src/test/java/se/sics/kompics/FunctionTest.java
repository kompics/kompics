/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author lkroll
 */
//@RunWith(JUnit4.class)
@SuppressWarnings("unused")
public class FunctionTest {

    public static interface HandlerFunc<T extends Event> extends Consumer<T>, Serializable {

    }

    @Test
    public void ftest() {
        Handler<Start> sH = handle(Start.event, e -> System.out.println("Event: " + e.toString()));
        Class sC = reflectHandlerEventType(sH);
        System.out.println("Event Class: " + sC.getName());
        sH.handle(Start.event);

        Handler<Start> ssH = handle(Start.event, this::handleStart);

        Class sC2 = reflectHandlerEventType(ssH);
        System.out.println("Event Class: " + sC2.getName());
        ssH.handle(Start.event);

    }

    public void handleStart(Start event) {
        System.out.println("Start Event: " + event.toString());
    }

    public <T extends Event> Handler<T> handle(T inst, HandlerFunc<T> fun) {
        System.out.println("Gen: " + fun.getClass().toGenericString());
        Method[] ifs = fun.getClass().getDeclaredMethods();
//        for (Method m : ifs) {
//            Type[] ts = m.toGenericString()
//            for (Type t : ts) {
//                System.out.println("Type: " + t.getTypeName());
//            }
//        }
        return new Handler<T>() {
            @Override
            public void handle(T event) {
                fun.accept(event);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> Class<E> reflectHandlerEventType(
            Handler<E> handler) {
        Class<E> eventType = null;
        try {
            Method declared[] = handler.getClass().getDeclaredMethods();
            // The JVM in Java 7 wrongly reflects the "handle" methods for some 
            // handlers: e.g. both `handle(Event e)` and `handle(Message m)` are
            // reflected as "declared" methods when only the second is actually
            // declared in the handler. A workaround is to reflect all `handle`
            // methods and pick the one with the most specific event type.
            // This sorted set stores the event types of all reflected handler
            // methods topologically ordered by the event type relationships.
            TreeSet<Class<? extends Event>> relevant
                    = new TreeSet<Class<? extends Event>>(
                            new Comparator<Class<? extends Event>>() {
                                @Override
                                public int compare(Class<? extends Event> e1,
                                        Class<? extends Event> e2) {
                                    if (e1.isAssignableFrom(e2)) {
                                        return 1;
                                    } else if (e2.isAssignableFrom(e1)) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
            for (Method m : declared) {
                if (m.getName().equals("handle")) {
                    relevant.add(
                            (Class<? extends Event>) m.getParameterTypes()[0]);
                }
            }
            eventType = (Class<E>) relevant.first();
        } catch (Exception e) {
            throw new RuntimeException("Cannot reflect handler event type for "
                    + "handler " + handler + ". Please specify it "
                    + "as an argument to the handler constructor.", e);
        } finally {
            if (eventType == null) {
                throw new RuntimeException(
                        "Cannot reflect handler event type for handler "
                        + handler + ". Please specify it "
                        + "as an argument to the handler constructor.");
            }
        }
        return eventType;
    }
}
