package se.sics.kompics.core;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.ListIterator;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.GuardedEventHandler;

public class EventHandlerCore {

	private EventHandler<Event> handler;
	private GuardedEventHandler<Event> guardedHandler;

	private Class<? extends Event> eventType;

	private LinkedList<Event> blockedEvents;

	private boolean guarded;

	@SuppressWarnings("unchecked")
	public EventHandlerCore(Class<? extends Event> eventType,
			EventHandler<? extends Event> handler) {
		this.eventType = eventType;
		/*
		 * the cast is safe since we check that we only pass events of the right
		 * type to this handler, by reflection at subscription time.
		 */
		this.handler = (EventHandler<Event>) handler;
		if (handler instanceof GuardedEventHandler) {
			this.guardedHandler = (GuardedEventHandler<Event>) handler;
		} else {
			this.guardedHandler = null;
		}

		if (guardedHandler == null) {
			this.guarded = false;
		} else {
			this.blockedEvents = new LinkedList<Event>();
			this.guarded = true;
		}
	}

	public Class<? extends Event> getEventType() {
		return eventType;
	}

	public boolean isGuarded() {
		return guarded;
	}

	public boolean hasBlockedEvents() {
		return (blockedEvents == null ? false : blockedEvents.size() > 0);
	}

	/**
	 * handles an event. If the handler is guarded, the guard is tested first.
	 * If the guard evaluates to false the event is enqueued locally
	 * 
	 * @param event
	 *            the event to be handled
	 * @return <code>true</code> if the handler was executed
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public boolean handleEvent(Event event) throws Throwable {
		if (guarded) {
			// test guard
			boolean allowed = guardedHandler.guard(event);
			if (allowed) {
				// handle event
				handler.handle(event);
				return true;
			} else {
				// enqueue event locally
				blockedEvents.addLast(event);
				return false;
			}
		} else {
			// handle event
			handler.handle(event);
			return true;
		}
	}

	/**
	 * @return <code>true</code> if one event was handled, <code>false</code> if
	 *         no blocked event could be executed.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public boolean handleOneBlockedEvent() throws Throwable {
		if (!guarded) {
			return false;
		}

		ListIterator<Event> iter = blockedEvents.listIterator();
		while (iter.hasNext()) {
			Event event = iter.next();

			boolean allow = (Boolean) guardedHandler.guard(event);
			if (allow) {
				handler.handle(event);
				iter.remove();
				return true;
			}
		}
		return false;
	}
}
