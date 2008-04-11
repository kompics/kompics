package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates the methods of a component that are event handler guard methods.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandlerGuardMethod {
}
