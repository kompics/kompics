package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import se.sics.kompics.api.Event;

/**
 * Annotates a component's event handler with the types of events that it may
 * trigger.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MayTriggerEventTypes {
	Class<? extends Event>[] value();
}
