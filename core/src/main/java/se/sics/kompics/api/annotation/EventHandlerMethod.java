package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;

/**
 * Annotates the event handler methods of a component.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
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
