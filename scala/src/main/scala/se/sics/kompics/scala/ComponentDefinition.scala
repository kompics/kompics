/**
 *
 */
package se.sics.kompics.scala
import se.sics.kompics.PortType
import se.sics.kompics.Positive
import se.sics.kompics.Negative
import se.sics.kompics.launch.Topology
import se.sics.kompics.PortCore
import se.sics.kompics.ControlPort
import se.sics.kompics.Event
import se.sics.kompics.Fault

/**
 * @author sario
 *
 */
abstract class ComponentDefinition() extends se.sics.kompics.ComponentDefinition(true) {
	private val core: DSLComponent = new DSLComponent(this);
	var faultHandler: (Event) => () => Unit = {case f: Fault => {() => 
		f.getFault().printStackTrace(System.err);
		}}
	
	lateInit(core);
	
//	def create[D <: ComponentDefiniton](definition: D): DSLComponent = {
//		core.doCreate(definition.getClass());
//	}
	
	def ++[P <: PortType](port: P): NegativePort[_ <: P] = {
		this ++ port.getClass();
	}
	
	def ++[P <: PortType](portType: Class[P]): NegativePort[_ <: P] = {
		val oldport = provides(portType);
		oldport match {
			case pc: PortCore[P] => return new NegativePort[P](pc)
			case _ => throw new ClassCastException
		}
	}
	
	def --[P <: PortType](port: P): PositivePort[_ <: P] = {
		this -- (port.getClass());
	}
	
	def --[P <: PortType](portType: Class[P]): PositivePort[_ <: P] = {
		val oldport = requires(portType);
		oldport match {
			case pc: PortCore[P] => return new PositivePort[P](pc)
			case _ => throw new ClassCastException
		}
	}
	
	def ctrl: NegativePort[ControlPort] = {
		control match {
			case pc: PortCore[ControlPort] => return new NegativePort[ControlPort](pc)
			case _ => throw new ClassCastException
		}
	}
	
	def init[T <: se.sics.kompics.ComponentDefinition](initEvent: Event, c: T): DSLComponent = {
		init(initEvent, c.getClass());
	}
	
	def init[T <: se.sics.kompics.ComponentDefinition](initEvent: Event, c: Class[T]) : DSLComponent = {
		val component = create(c);
		component match {
			case comp: DSLComponent => {
				comp.ctrl uponEvent faultHandler;
				trigger(initEvent, comp.ctrl);
				return comp;
			}
			case _ => throw new ClassCastException
		}
	}
	
}

object ComponentDefinition {
		
}