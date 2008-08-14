package se.sics.kompics.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;

import se.sics.kompics.api.Event;

/**
 * The <code>SubscriptionSet</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: SubscriptionSet.java 117 2008-05-30 15:38:29Z cosmin $
 */
public class SubscriptionSet {

	final Class<? extends Event> eventType;

	LinkedList<Subscription> noFilterSubs;

	LinkedList<Subscription> manyFilterSubs;

	HashMap<Field, HashMap<Object, LinkedList<Subscription>>> oneFilterSubs;

	public SubscriptionSet(Class<? extends Event> eventType) {
		this.eventType = eventType;
		this.noFilterSubs = null;
		this.manyFilterSubs = null;
		this.oneFilterSubs = null;
	}

	/* called only for one filter subs */
	public LinkedList<Subscription> getSubscriptions(Field field, Object value) {
		HashMap<Object, LinkedList<Subscription>> valueSubs = oneFilterSubs
				.get(field);
		if (valueSubs == null) {
			return null;
		}
		return valueSubs.get(value);
	}

	public void addSubscription(Subscription sub) {
		if (sub.getFilters().length == 0) {
			// no filters
			if (noFilterSubs == null) {
				noFilterSubs = new LinkedList<Subscription>();
			}
			noFilterSubs.add(sub);
		} else if (sub.getFilters().length > 1) {
			// more than one filter
			if (manyFilterSubs == null) {
				manyFilterSubs = new LinkedList<Subscription>();
			}
			manyFilterSubs.add(sub);
		} else {
			// exactly one filter
			if (oneFilterSubs == null) {
				oneFilterSubs = new HashMap<Field, HashMap<Object, LinkedList<Subscription>>>();
			}
			Field field = sub.getFilters()[0].getAttribute();
			HashMap<Object, LinkedList<Subscription>> valueSubs = oneFilterSubs
					.get(field);
			if (valueSubs == null) {
				valueSubs = new HashMap<Object, LinkedList<Subscription>>();
				oneFilterSubs.put(field, valueSubs);
			}
			Object value = sub.getFilters()[0].getValue();
			LinkedList<Subscription> subs = valueSubs.get(value);
			if (subs == null) {
				subs = new LinkedList<Subscription>();
				valueSubs.put(value, subs);
			}
			subs.add(sub);
		}
	}

	public void removeSubscription(Subscription sub) {
		if (sub.getFilters().length == 0) {
			// no filters
			if (noFilterSubs != null) {
				noFilterSubs.remove(sub);
				if (noFilterSubs.isEmpty()) {
					noFilterSubs = null;
				}
			}
		} else if (sub.getFilters().length > 1) {
			// more than one filter
			if (manyFilterSubs != null) {
				manyFilterSubs.remove(sub);
				if (manyFilterSubs.isEmpty()) {
					manyFilterSubs = null;
				}
			}
		} else {
			// exactly one filter
			if (oneFilterSubs != null) {
				Field field = sub.getFilters()[0].getAttribute();
				HashMap<Object, LinkedList<Subscription>> valueSubs = oneFilterSubs
						.get(field);
				if (valueSubs != null) {
					Object value = sub.getFilters()[0].getValue();
					LinkedList<Subscription> subs = valueSubs.get(value);
					if (subs != null) {
						subs.remove(sub);
						if (subs.size() == 0) {
							valueSubs.remove(value);
							if (valueSubs.size() == 0) {
								oneFilterSubs.remove(field);
								if (oneFilterSubs.size() == 0) {
									oneFilterSubs = null;
								}
							}
						}
					}
				}
			}
		}
	}
}
