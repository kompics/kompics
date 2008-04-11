package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates Kompics component types.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentType {
	/**
	 * @return <code>true</code>if the component is composite.
	 */
	boolean composite() default false;
}
