/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.ComponentCore
import se.sics.kompics.Positive
import se.sics.kompics.PortType
import se.sics.kompics.PortCore
import se.sics.kompics.ControlPort

/**
 * @author sario
 *
 */
class DSLComponent(cd: ComponentDefinition) extends ComponentCore(cd) {
	
	def ++[P <: PortType](port: P): PositivePort[_ <: P] = {
		this ++ port.getClass();
	}
	
	def ++[P <: PortType](portType: Class[P]): PositivePort[_ <: P] = {
		val oldport = provided(portType);
		oldport match {
			case pc: PortCore[P] => return new PositivePort[P](pc)
			case _ => throw new ClassCastException
		}
	}
	
	def --[P <: PortType](port: P): NegativePort[_ <: P] = {
		this -- (port.getClass());
	}
	
	def --[P <: PortType](portType: Class[P]): NegativePort[_ <: P] = {
		val oldport = required(portType);
		oldport match {
			case pc: PortCore[P] => return new NegativePort[P](pc)
			case _ => throw new ClassCastException
		}
	}
	
	def ctrl: PositivePort[ControlPort] = {
		control match {
			case pc: PortCore[ControlPort] => return new PositivePort[ControlPort](pc)
			case _ => throw new ClassCastException
		}
	}
}

object DSLComponent {
	//implicit def component2DSL(cc: ComponentCore): DSLComponent = new DSLComponent(cc);
}