package se.sics.kompics.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates the <code>create</code> method of a component.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentCreateMethod {
}
