package se.sics.kompics.scala.propose

import se.sics.kompics.Event
import se.sics.kompics.address.Address

case class Pp2pSend(val d: Address, val m: Any) extends Event {

}