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
import java.util.concurrent.locks.ReentrantReadWriteLock
import se.sics.kompics.ComponentCore
import se.sics.kompics.PortType
import se.sics.kompics.PortCore
import se.sics.kompics.Event
import se.sics.kompics.SpinlockQueue
import se.sics.kompics.ChannelCore
import se.sics.kompics.ChannelFilterSet
import se.sics.kompics.ConfigurationException
import se.sics.kompics.Handler
import se.sics.kompics.Request
import se.sics.kompics.Response
import se.sics.kompics.Fault
import se.sics.kompics.ChannelFilter
import se.sics.kompics.Component
import se.sics.kompics.Channel
import se.sics.kompics.Positive
import se.sics.kompics.Negative
import se.sics.kompics.RequestPathElement
import se.sics.kompics.Port
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.SetBuilder
import scala.reflect.Manifest
import java.lang.reflect.Method

/**
 * The <code>ScalaPort</code> class.
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id: $
 */
class ScalaPort[P <: PortType](positive: Boolean, pType: P, parent: ComponentCore) extends PortCore[P] with NegativePort[P] with PositivePort[P] {
	
	isPositive = positive;
	portType = pType;
	owner = parent;

	private var subscriptions: Set[(Event) => () => Unit] = Set[(Event) => () => Unit]().empty;

	private var pair: ScalaPort[P] = null;

	private val eventQueue: SpinlockQueue[Event] = new SpinlockQueue[Event]();
	private val preparedHandlers: HashMap[Event, Set[() => Unit]] = new HashMap[Event, Set[() => Unit]];

	private var rwLock: ReentrantReadWriteLock = new ReentrantReadWriteLock();

	private var allChannels: Set[ChannelCore[P]] = Set[ChannelCore[P]]();
	private var unfilteredChannels: Set[ChannelCore[P]] = Set[ChannelCore[P]]();
	private var filteredChannels: ChannelFilterSet = new ChannelFilterSet();

	private var remotePorts: Map[Port[P], ChannelCore[P]] = Map[Port[P], ChannelCore[P]]();

	override def getPair(): PortCore[P] = {
		return pair;
	}

	override def setPair(port: PortCore[P]): Unit = {
		port match {
			case sp: ScalaPort[P] => pair = sp;
			case _ => throw new ConfigurationException("Can only pair up this port with another ScalaPort instance");
		}
	}
	
	override def doSubscribe[E <: Event](handler: Handler[E]): Unit = {
		var eventType = handler.getEventType();
		if (eventType == null) {
			eventType = reflectHandlerEventType(handler);
			handler.setEventType(eventType);
		}
		val closureHandler: (Event) => () => Unit = {e => if (e.getClass().isAssignableFrom(eventType)) () => { handler.asInstanceOf[Handler[Event]].handle(e) } else {
			throw new MatchError(e.getClass()+ " didn't match "+ eventType);
		}};
		doSubscribe(closureHandler);
	}
	
	private def reflectHandlerEventType[E <: Event] (handler: Handler[E]): Class[E] = {
		var eventType: Class[E] = null;
		try {
			val ms = handler.getClass().getDeclaredMethods();
			var m: Method = null;
			for (m1 <- ms) {
				if (m1.getName().equals("handle")) {
					m = m1;
				}
			}
			eventType = m.getParameterTypes()(0) match {case ce: Class[E] => ce};
		} catch {
			case e => throw new RuntimeException("Cannot reflect handler event type for "
					+ "handler " + handler + ". Please specify it "
					+ "as an argument to the handler constructor.", e);
		} finally {
			if (eventType == null)
				throw new RuntimeException(
						"Cannot reflect handler event type for handler "
								+ handler + ". Please specify it "
								+ "as an argument to the handler constructor.");
		}
		return eventType;
	}

	private def doManifestSubscribe[E <: Event: Manifest](handler: Handler[E]): Unit = {
		val closureHandler: (Event) => () => Unit = { case e: E => () => { handler.handle(e) } };
		doSubscribe(closureHandler);
	}

	def doSubscribe(handler: (Event) => () => Unit): Unit = {
		subscriptions += handler;
	}

	def uponEvent(handler: (Event) => () => Unit): Unit = doSubscribe(handler);

	def getMatchingHandlers(event: Event): Set[() => Unit] = {
		val matching = new SetBuilder[() => Unit, Set[() => Unit]](Set().empty);
		subscriptions.foreach(handler => {
			try {
				matching += handler(event);
			} catch {
				case e: MatchError => //ignore (not all handlers usually match) //println("MatchError: "+e.getMessage());
			}
		});
		return matching.result();
	}

	def pickFirstEvent(): Event = {
		return eventQueue.poll();
	}
	
	def pollPreparedHandlers(event: Event): Set[() => Unit] = {
		if (preparedHandlers contains event) {
			val ph = preparedHandlers(event);
			preparedHandlers -= event;
			return ph;
		} else {
			return getMatchingHandlers(event);
		}
	}

	override def doTrigger(event: Event, wid: Int, channel: ChannelCore[_]): Unit = {
//		println(this.getClass()+": "+event+" triggert from "+channel);
		event match {
			case r: Request => r.pushPathElement(channel);
			case _ =>
		}
		pair.deliver(event, wid);
	}

	override def doTrigger(event: Event, wid: Int, component: ComponentCore): Unit = {
//		println(this.getClass()+": "+event+" triggert from "+component);
		event match {
			case r: Request => r.pushPathElement(component);
			case _ =>
		}
		pair.deliver(event, wid);
	}

	private def deliver(event: Event, wid: Int): Unit = {
		val eventType: Class[_ <: Event] = event.getClass();
		var delivered = false;

		rwLock.readLock().lock();
		try {
			event match {
				case response: Response => {
					val pe: RequestPathElement = response.getTopPathElement();
					if (pe != null) {
						if (pe.isChannel()) {
							pe.getChannel() match {
								case caller: ChannelCore[_] => {
									if (caller != null) {
										// caller can be null since it is a WeakReference
										delivered = deliverToCaller(event, wid, caller);
									}
								}
							}
						} else {
							val component = pe.getComponent();
							if (component == owner) {
								delivered = deliverToSubscribers(event, wid);
							} else {
								throw new RuntimeException(
									"Response path invalid: expected to arrive to component "
										+ component.getComponent()
										+ " but instead arrived at "
										+ owner.getComponent());
							}
						}
					} else {
						// response event has arrived to request origin and was
						// triggered further. We treat it as a regular event
						// TODO actually handle the continuation
						delivered = deliverToSubscribers(event, wid);
						delivered |= deliverToChannels(event, wid);
					}
				}
				case _ => {
					delivered = deliverToSubscribers(event, wid);
					delivered |= deliverToChannels(event, wid);
				}
			}
		} finally {
			rwLock.readLock().unlock();
		}

		if (!delivered) {
			if (pType.hasEvent(isPositive, eventType)) {
				event match {
					case f: Fault => {
						if (owner.getParent() != null) {
							owner.getComponent().getControlPort() match {
								case pc: PortCore[_] => pc.doTrigger(f, wid, owner.getComponent().getComponentCore());
							}
						} else {
							owner.handleFault(f.getFault());
						}
					}
					case _ => println("Warning: Dropped Event "+event);
				}
			} else {
				// error, event type doesn't flow on this port in this direction
				throw new RuntimeException(eventType.getCanonicalName()
					+ " events cannot be triggered on "
					+ portDirection(!isPositive) + " "
					+ portType.getClass().getCanonicalName());
			}
		}
	}

	private def deliverToChannels(event: Event, wid: Int): Boolean = {
//		print(event+" trying to deliver to channels...");
		var delivered = false;
		unfilteredChannels.foreach(cc => {
			if (isPositive) {
				cc.forwardToNegative(event, wid);
			} else {
				cc.forwardToPositive(event, wid)
			}
			delivered = true;
		})
		val channels = filteredChannels.get(event).asScala;
		channels.foreach(cc => {
			if (isPositive) {
				cc.forwardToNegative(event, wid);
			} else {
				cc.forwardToPositive(event, wid)
			}
			delivered = true;
		})
//		if (delivered) println("succeeded") else println("failed");
		return delivered;
	}

	private def deliverToCaller(event: Event, wid: Int, caller: ChannelCore[_]): Boolean = {
//		println(event+" forwarding to caller.");
		if (isPositive) {
			caller.forwardToNegative(event, wid);
		} else {
			caller.forwardToPositive(event, wid);
		}
		return true;
	}

	private def deliverToSubscribers(event: Event, wid: Int): Boolean = {
//		print(event+" trying to deliver to subscribers...");
		val isInitEvent = event.isInstanceOf[se.sics.kompics.Init];
		if (isInitEvent) {
			// On init events just notify the component. It will find matching handlers itself.
			owner.eventReceived(this, event, wid, isControlPort);
//			println("succeded");
			return true;
		} else {
			if (!subscriptions.isEmpty) {
				val handlers = getMatchingHandlers(event);
				if (!handlers.isEmpty) {
					preparedHandlers += (event -> handlers);
					owner.eventReceived(this, event, wid, false);
//					println("succeded");
					return true;
				}
			}
		}
//		println("failed");
		return false;
	}

	private def portDirection(): String = {
		portDirection(isPositive);
	}

	private def portDirection(pos: Boolean): String = {
		if (pos) {
			return "positive";
		} else {
			return "negative";
		}
	}

	override def addChannel(channel: ChannelCore[P]): Unit = {
		val remotePort: Port[P] = {
			if (isPositive)
				channel.getNegativePort() else channel.getPositivePort()
		};
		if (remotePorts.contains(remotePort)) {
			throw new RuntimeException({ if (isPositive) "Positive " else "Negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ pair.getOwner().getComponent() + " is already connected to "
				+ { if (!isPositive) "positive " else "negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ remotePort.getPair().getOwner().getComponent());
		}
		rwLock.writeLock().lock();
		try {
			allChannels += channel;
			unfilteredChannels += channel;
			remotePorts += (remotePort -> channel);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	override def addChannel(channel: ChannelCore[P], filter: ChannelFilter[_, _]): Unit = {
		val remotePort: Port[P] = {
			if (isPositive)
				channel.getNegativePort() else channel.getPositivePort()
		};
		if (remotePorts.contains(remotePort)) {
			throw new RuntimeException({ if (isPositive) "Positive " else "Negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ pair.getOwner().getComponent() + " is already connected to "
				+ { if (!isPositive) "positive " else "negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ remotePort.getPair().getOwner().getComponent());
		}
		rwLock.writeLock().lock();
		try {
			allChannels += channel;
			filteredChannels.addChannelFilter(channel, filter);
			remotePorts += (remotePort -> channel);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	override def removeChannelTo(remotePort: PortCore[P]): Unit = {
		if (!remotePorts.contains(remotePort)) {
			throw new RuntimeException({ if (isPositive) "Positive " else "Negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ pair.getOwner().getComponent() + " is not connected to "
				+ { if (!isPositive) "positive " else "negative " }
				+ portType.getClass().getCanonicalName() + " of "
				+ remotePort.getPair().getOwner().getComponent());
		}
		rwLock.writeLock().lock();
		try {
			val channel = remotePorts(remotePort);
			remotePorts -= remotePort;
			allChannels -= channel;
			unfilteredChannels -= channel;
			filteredChannels.removeChannel(channel);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	override def enqueue(event: Event): Unit = {
		eventQueue.offer(event);
	}

	def ++(component: Component): Channel[P] = {
		val positivePort: Positive[_ <: P] = component.getPositive(this.getPortType().getClass());
		positivePort match {
			case pos: PortCore[P] => {
				val channel = new ChannelCore[P](pos, this, this.getPortType());
				this.addChannel(channel);
				pos.addChannel(channel);
				return channel;
			}
			case _ => throw new ClassCastException()
		}
	}

	def ++(components: Component*): Seq[Channel[P]] = {
		components.map(++);
	}

	def --(component: Component): Channel[P] = {
		val negativePort: Negative[_ <: P] = component.getNegative(this.getPortType().getClass());
		negativePort match {
			case neg: PortCore[P] => {
				val channel = new ChannelCore[P](this, neg, this.getPortType());
				this.addChannel(channel);
				neg.addChannel(channel);
				return channel;
			}
			case _ => throw new ClassCastException()
		}
	}

	def --(components: Component*): Seq[Channel[P]] = {
		components.map(--);
	}
}

object ScalaPort {

	def fromPort[P <: PortType](other: ScalaPort[P]): ScalaPort[P] = {
		val port = new ScalaPort(other.isPositive, other.portType, other.owner);
		port.rwLock = other.rwLock;
		return port;
	}
}