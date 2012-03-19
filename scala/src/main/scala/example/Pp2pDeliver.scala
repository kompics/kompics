/**
 *
 */
package se.sics.kompics.scala.propose

import se.sics.kompics.Event
import se.sics.kompics.address.Address

/**
 * @author sario
 *
 */
case class Pp2pDeliver(val source: Address, val m: Any) extends Event {

}