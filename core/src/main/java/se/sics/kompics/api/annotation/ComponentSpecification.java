package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates Kompics component specifications.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentSpecification {
	/**
	 * @return <code>true</code>if the component is composite.
	 */
	boolean composite() default false;
}
