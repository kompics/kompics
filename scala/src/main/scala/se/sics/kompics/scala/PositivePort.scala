/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.PortCore
import se.sics.kompics.PortType
import se.sics.kompics.Positive
import se.sics.kompics.Channel
import se.sics.kompics.Negative
import se.sics.kompics.ChannelCore

/**
 * @author sario
 *
 */
class PositivePort[P <: PortType](pc: PortCore[P]) extends PortCore[P](pc) with ClosureEventHandler {
	def --(component: DSLComponent): Channel[P] = {
		val negativePort:Negative[_ <: P] = component.getNegative(this.getPortType().getClass());
		negativePort match {
			case neg: PortCore[P] => {
				val channel:Channel[P] = new ChannelCore[P](this, neg, this.getPortType());
				return channel;
			}
			case _ => throw new ClassCastException()
		}
	}
	
	def --(components: DSLComponent*): Seq[Channel[P]] = {
		components.map(--);
	}
}

object PositivePort {
	implicit def port2positiveport[P <: PortType](pc: PortCore[P]): PositivePort[P] = new PositivePort[P](pc);
	implicit def positive2positiveport[P <: PortType](pos: Positive[P]): PositivePort[P] = {
		pos match {
			case pc: PortCore[P] => new PositivePort[P](pc)
			case _ => throw new ClassCastException()
		}
	}
}