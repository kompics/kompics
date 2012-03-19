/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.PortCore
import se.sics.kompics.PortType
import se.sics.kompics.Negative
import se.sics.kompics.Channel
import se.sics.kompics.ChannelCore
import se.sics.kompics.Positive

/**
 * @author sario
 *
 */
class NegativePort[P <: PortType](pc: PortCore[P]) extends PortCore[P](pc) with ClosureEventHandler {
	def ++(component: DSLComponent): Channel[P] = {
		val positivePort:Positive[_ <: P] = component.getPositive(this.getPortType().getClass());
		positivePort match {
			case pos: PortCore[P] => {
				val channel:Channel[P] = new ChannelCore[P](pos, this, this.getPortType());
				return channel;
			}
			case _ => throw new ClassCastException()
		}
	}
	
	def ++(components: DSLComponent*): Seq[Channel[P]] = {
		components.map(++);
	}
	
	
}

object NegativePort {
	implicit def port2negativeport[P <: PortType](pc: PortCore[P]): NegativePort[P] = new NegativePort[P](pc);
	implicit def negative2negativeport[P <: PortType](neg: Negative[P]): NegativePort[P] = {
		neg match {
			case pc: PortCore[P] => new NegativePort[P](pc)
			case _ => throw new ClassCastException()
		}
	}
}