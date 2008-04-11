package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates the event handler methods of a component.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandlerMethod {

	/**
	 * @return <code>true</code> if the event handler is guarded
	 */
	boolean guarded() default false;

	/**
	 * @return the name of the guard method when the event handler is guarded
	 */
	String guard() default "";
}
