/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.scala

import scala.collection.JavaConverters._
import se.sics.kompics.ComponentCore
import se.sics.kompics.PortType
import se.sics.kompics.ControlPort
import se.sics.kompics.Component
import se.sics.kompics.ConfigurationException
import se.sics.kompics.Negative
import se.sics.kompics.Event
import se.sics.kompics.Start
import se.sics.kompics.Stop
import se.sics.kompics.PortCore
import java.util.LinkedList
import se.sics.kompics.Positive
import se.sics.kompics.Fault
import se.sics.kompics.SpinlockQueue
import se.sics.kompics.Kompics
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The <code>ScalaComponent</code> class.
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id: $
 */
class ScalaComponent extends ComponentCore {

	var component: ComponentDefinition = null;

	var positivePorts: Map[Class[_ <: PortType], ScalaPort[_ <: PortType]] = Map[Class[_ <: PortType], ScalaPort[_ <: PortType]]().empty;
	var negativePorts: Map[Class[_ <: PortType], ScalaPort[_ <: PortType]] = Map[Class[_ <: PortType], ScalaPort[_ <: PortType]]().empty;

	var negativeControl: ScalaPort[ControlPort] = null;
	var positiveControl: ScalaPort[ControlPort] = null;

	this.initSubscriptionInConstructor = false;
	this.initDone = new AtomicBoolean(false);
	this.initReceived = new AtomicBoolean(false);
	this.firstInitEvent = new AtomicReference[Event](null);
	ComponentCore.parentThreadLocal.set(null);

	override def doCreate(definition: Class[_ <: se.sics.kompics.ComponentDefinition]): Component = {
		try {
			ComponentCore.parentThreadLocal.set(this);
			val componentDefinition = definition.newInstance();
			val child: ComponentCore = componentDefinition.getComponentCore();

			if (!child.initSubscriptionInConstructor) {
				child.initDone.set(true);
			} else {
				child.workCount.incrementAndGet();
			}

			child.setScheduler(scheduler);
			if (children == null) {
				children = new LinkedList[ComponentCore]();
			}
			children.add(child);

			return child;
		} catch {
			case e: InstantiationException =>
				throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
			case e: IllegalAccessException =>
				throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
		}
	}

	override def createNegativePort[P <: PortType](portType: Class[P]): Negative[P] = {
		if (!positivePorts.contains(portType)) {
			val pType = PortType.getPortType(portType);
			val positivePort = new ScalaPort[P](true, pType, parent);
			val negativePort = new ScalaPort[P](false, pType, this);

			negativePort.setPair(positivePort);
			positivePort.setPair(negativePort);

			positivePorts += (portType -> positivePort);

			return negativePort;
		}
		throw new RuntimeException("Cannot create multiple negative "
			+ portType.getCanonicalName());
	}

	override def createPositivePort[P <: PortType](portType: Class[P]): Positive[P] = {
		if (!negativePorts.contains(portType)) {
			val pType = PortType.getPortType(portType);
			val positivePort = new ScalaPort[P](true, pType, this);
			val negativePort = new ScalaPort[P](false, pType, parent);

			negativePort.setPair(positivePort);
			positivePort.setPair(negativePort);

			negativePorts += (portType -> negativePort);

			return positivePort;
		}
		throw new RuntimeException("Cannot create multiple positive "
			+ portType.getCanonicalName());
	}

	override def createControlPort(): Negative[ControlPort] = {

		val controlPortType = PortType.getPortType(classOf[ControlPort]);
		negativeControl = new ScalaPort[ControlPort](false, controlPortType, this);
		positiveControl = new ScalaPort[ControlPort](true, controlPortType, parent);

		positiveControl.setPair(negativeControl);
		negativeControl.setPair(positiveControl);

		negativeControl.doSubscribe(startHandler);
		negativeControl.doSubscribe(stopHandler);

		return negativeControl;
	}

	override def getPositive[P <: PortType](portType: Class[P]): Positive[P] = {
		if (positivePorts.contains(portType)) {
			positivePorts(portType) match {
				case pos: Positive[P] => return pos
				case _ =>
			}
		}
		throw new RuntimeException(component + " has no positive "
			+ portType.getCanonicalName());
	}

	override def provided[P <: PortType](portType: Class[P]): Positive[P] = getPositive(portType);

	override def getNegative[P <: PortType](portType: Class[P]): Negative[P] = {
		if (negativePorts.contains(portType)) {
			negativePorts(portType) match {
				case neg: Negative[P] => return neg
				case _ =>
			}
		}
		throw new RuntimeException(component + " has no positive "
			+ portType.getCanonicalName());
	}

	override def required[P <: PortType](portType: Class[P]): Negative[P] = getNegative(portType);

	var startHandler: (Event) => () => Unit = {
		case s: Start => { () =>
			if (children != null) {
				for (child: ComponentCore <- children.asScala) {
					child.getControl() match {
						case pc: PortCore[ControlPort] => pc.doTrigger(Start.event, wid, component.getComponentCore())
						case p => throw new RuntimeException("Unknown port type: " + p.getClass().getCanonicalName());
					}
				}
			}
		}
	}

	var stopHandler: (Event) => () => Unit = {
		case s: Stop => { () =>
			if (children != null) {
				for (child: ComponentCore <- children.asScala) {
					child.getControl() match {
						case pc: PortCore[ControlPort] => pc.doTrigger(Stop.event, wid, component.getComponentCore())
						case p => throw new RuntimeException("Unknown port type: " + p.getClass().getCanonicalName());
					}
				}
			}
		}
	}

	override def execute(wid: Int): Unit = {
		import Kompics._
		this.wid = wid;

		// if init is not yet done it means we were scheduled to run the first
		// Init event. We do not touch readyPorts and workCount.
		if (!initDone.get()) {
			val handlers = negativeControl.getMatchingHandlers(firstInitEvent.get());
			if (handlers != null) {
				handlers foreach {
					handler => handler()
				}
			}
			initDone.set(true);

			val wc: Int = workCount.decrementAndGet();
			if (wc > 0) {
				if (scheduler == null) {
					scheduler = getScheduler();
				}
				scheduler.schedule(this, wid);
			}
			return ;
		}

		// 1. pick a port with a non-empty handler queue
		// 2. execute the first handler
		// 3. make component ready

		val nextPort: ScalaPort[_] = readyPorts.poll() match {
			case sp: ScalaPort[_] => sp
			case _ => throw new RuntimeException("Incompatible port type")
		}

		val event: Event = nextPort.pickFirstEvent();

		val handlers = nextPort.pollPreparedHandlers(event);

		if (handlers != null) {
			handlers foreach {
				handler => handler()
			}
		}

		val wc: Int = workCount.decrementAndGet();
		if (wc > 0) {
			if (scheduler == null)
				scheduler = getScheduler();
			scheduler.schedule(this, wid);
		}
	}

	def ++[P <: PortType](port: P): PositivePort[_ <: P] = {
		this ++ port.getClass();
	}

	def ++[P <: PortType](portType: Class[P]): PositivePort[_ <: P] = {
		val port = provided(portType);
		port match {
			case pc: PositivePort[P] => return pc;
			case _ => throw new ClassCastException
		}
	}

	def --[P <: PortType](port: P): NegativePort[_ <: P] = {
		this -- (port.getClass());
	}

	def --[P <: PortType](portType: Class[P]): NegativePort[_ <: P] = {
		val port = required(portType);
		port match {
			case pc: NegativePort[P] => return pc;
			case _ => throw new ClassCastException
		}
	}

	override def getControl(): Positive[ControlPort] = {
		return positiveControl;
	}

	override def control(): Positive[ControlPort] = {
		return positiveControl;
	}

	override def getComponent(): se.sics.kompics.ComponentDefinition = {
		return component;
	}

	override def handleFault(throwable: Throwable): Unit = {
		if (parent != null) {
			negativeControl.doTrigger(new Fault(throwable), wid, this);
		} else {
			throw new RuntimeException("Kompics isolated fault ", throwable);
		}
	}

	def ctrl: PositivePort[ControlPort] = {
		return positiveControl;
	}
}

class ScalaComponentWrapper(component: Component) {

	def ++[P <: PortType](port: P): PositivePort[_ <: P] = {
		this ++ port.getClass();
	}

	def ++[P <: PortType](portType: Class[P]): PositivePort[_ <: P] = {
		val port = component.provided(portType);
		port match {
			case pp: PositivePort[P] => return pp;
			case pc: PortCore[P] => return new PositiveWrapper[P](pc);
		}
	}

	def --[P <: PortType](port: P): NegativePort[_ <: P] = {
		this -- (port.getClass());
	}

	def --[P <: PortType](portType: Class[P]): NegativePort[_ <: P] = {
		val port = component.required(portType);
		port match {
			case np: NegativePort[P] => return np;
			case pc: PortCore[P] => return new NegativeWrapper[P](pc);
		}
	}
}

/**
 * The <code>ScalaComponent</code> object.
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id: $
 */
object ScalaComponent {
	implicit def component2Scala(cc: Component): ScalaComponentWrapper = new ScalaComponentWrapper(cc);
}