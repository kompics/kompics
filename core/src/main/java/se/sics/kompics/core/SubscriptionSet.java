package se.sics.kompics.core;

import java.lang.reflect.Field;
import java.util.HashMap;

import se.sics.kompics.api.Event;

/**
 * The <code>SubscriptionSet</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: SubscriptionSet.java 117 2008-05-30 15:38:29Z cosmin $
 */
public class SubscriptionSet {

	final Class<? extends Event> eventType;

	Subscription[] noFilterSubs;

	Subscription[] manyFilterSubs;

	HashMap<Field, HashMap<Object, Subscription[]>> oneFilterSubs;

	public SubscriptionSet(Class<? extends Event> eventType) {
		this.eventType = eventType;
		this.noFilterSubs = null;
		this.manyFilterSubs = null;
		this.oneFilterSubs = null;
	}

	/* called only for one filter subs */
	public Subscription[] getSubscriptions(Field field, Object value) {
		HashMap<Object, Subscription[]> valueSubs = oneFilterSubs.get(field);
		if (valueSubs == null) {
			return null;
		}
		Subscription[] subs = valueSubs.get(value);
		return subs;
	}

	public void addSubscription(Subscription sub) {
		if (sub.getFilters().length == 0) {
			// no filters
			noFilterSubs = addToArray(noFilterSubs, sub);
		} else if (sub.getFilters().length > 1) {
			// more than one filter
			manyFilterSubs = addToArray(manyFilterSubs, sub);
		} else {
			// exactly one filter
			if (oneFilterSubs == null) {
				oneFilterSubs = new HashMap<Field, HashMap<Object, Subscription[]>>();
			}
			Field field = sub.getFilters()[0].getAttribute();
			HashMap<Object, Subscription[]> valueSubs = oneFilterSubs
					.get(field);
			if (valueSubs == null) {
				valueSubs = new HashMap<Object, Subscription[]>();
				oneFilterSubs.put(field, valueSubs);
			}
			Object value = sub.getFilters()[0].getValue();
			Subscription[] subs = valueSubs.get(value);
			subs = addToArray(subs, sub);
			valueSubs.put(value, subs);
		}
	}

	public void removeSubscription(Subscription sub) {
		if (sub.getFilters().length == 0) {
			// no filters
			noFilterSubs = removeFromArray(noFilterSubs, sub);
		} else if (sub.getFilters().length > 1) {
			// more than one filter
			manyFilterSubs = removeFromArray(manyFilterSubs, sub);
		} else {
			// exactly one filter
			if (oneFilterSubs != null) {
				Field field = sub.getFilters()[0].getAttribute();
				HashMap<Object, Subscription[]> valueSubs = oneFilterSubs
						.get(field);
				if (valueSubs != null) {
					Object value = sub.getFilters()[0].getValue();
					Subscription[] subs = valueSubs.get(value);
					subs = removeFromArray(subs, sub);
					if (subs == null) {
						valueSubs.remove(value);
						if (valueSubs.size() == 0) {
							oneFilterSubs.remove(field);
							if (oneFilterSubs.size() == 0) {
								oneFilterSubs = null;
							}
						}
					} else {
						valueSubs.put(value, subs);
					}
				}
			}
		}
	}

	private Subscription[] addToArray(Subscription[] array, Subscription s) {
		if (array == null) {
			array = new Subscription[1];
			array[0] = s;
		} else {
			Subscription[] temp = array;
			array = new Subscription[temp.length + 1];
			for (int i = 0; i < temp.length; i++) {
				array[i] = temp[i];
			}
			array[array.length - 1] = s;
		}
		return array;
	}

	private Subscription[] removeFromArray(Subscription[] array, Subscription s) {
		if (array != null) {
			int rem = Integer.MAX_VALUE;
			for (int i = 0; i < array.length; i++) {
				if (array[i].equals(s)) {
					rem = i;
					break;
				}
			}
			if (array.length == 1 && rem == 0) {
				return null;
			}
			Subscription[] temp = array;
			array = new Subscription[temp.length - 1];
			for (int i = 0; i < rem; i++) {
				array[i] = temp[i];
			}
			for (int i = rem; i < temp.length - 1; i++) {
				array[i] = temp[i + 1];
			}
		}
		return array;
	}
}
