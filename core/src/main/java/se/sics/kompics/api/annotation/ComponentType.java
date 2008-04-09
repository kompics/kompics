package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;

/**
 * Annotates Kompics component types.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
public @interface ComponentType {
	/**
	 * @return specifies whether the component is composite or not
	 */
	boolean composite() default false;
}
