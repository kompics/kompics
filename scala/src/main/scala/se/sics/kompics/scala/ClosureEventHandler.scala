/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.Event
import scala.collection.mutable.ListBuffer

/**
 * @author sario
 *
 */
trait ClosureEventHandler {
	
	def uponEvent(check: (Event) => () => Unit) = {
		// put check on a list
		// whenever an event e arrives save the return of check(a) into a val
		// if no exception is thrown, (i.e. it matches), then put the result on a handling queue
		// else ignore and proceed with the next check
	}
}