package se.sics.kompics.core;

import java.lang.reflect.Field;
import java.util.HashMap;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.FastEventFilter;

/**
 * The <code>SubscriptionSet</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: SubscriptionSet.java 117 2008-05-30 15:38:29Z cosmin $
 */
public class SubscriptionSet {

	final Class<? extends Event> eventType;

	Subscription[] noFilterSubs;

	Subscription[] slowFilterSubs;

	HashMap<Field, HashMap<Object, Subscription[]>> fastFilterSubs;

	public SubscriptionSet(Class<? extends Event> eventType) {
		this.eventType = eventType;
		this.noFilterSubs = null;
		this.slowFilterSubs = null;
		this.fastFilterSubs = null;
	}

	/* called only for one filter subs */
	public Subscription[] getSubscriptions(Field field, Object value) {
		HashMap<Object, Subscription[]> valueSubs = fastFilterSubs.get(field);
		if (valueSubs == null) {
			return null;
		}
		Subscription[] subs = valueSubs.get(value);
		return subs;
	}

	public void addSubscription(Subscription sub) {
		if (sub.getEventFilter() == null) {
			// no filters
			noFilterSubs = addToArray(noFilterSubs, sub);
		} else if (sub.getEventFilter() instanceof FastEventFilter) {
			// fast filter
			if (fastFilterSubs == null) {
				fastFilterSubs = new HashMap<Field, HashMap<Object, Subscription[]>>();
			}
			FastEventFilter<? extends Event> fastEventFilter = (FastEventFilter<? extends Event>) sub
					.getEventFilter();
			Field field = sub.getField();
			HashMap<Object, Subscription[]> valueSubs = fastFilterSubs
					.get(field);
			if (valueSubs == null) {
				valueSubs = new HashMap<Object, Subscription[]>();
				fastFilterSubs.put(field, valueSubs);
			}
			Object value = fastEventFilter.getValue();
			Subscription[] subs = valueSubs.get(value);
			subs = addToArray(subs, sub);
			valueSubs.put(value, subs);
		} else {
			// slow filter filter
			slowFilterSubs = addToArray(slowFilterSubs, sub);
		}
	}

	public void removeSubscription(Subscription sub) {
		if (sub.getEventFilter() == null) {
			// no filters
			noFilterSubs = removeFromArray(noFilterSubs, sub);
		} else if (sub.getEventFilter() instanceof FastEventFilter) {
			// fast filter
			if (fastFilterSubs != null) {
				Field field = sub.getField();
				HashMap<Object, Subscription[]> valueSubs = fastFilterSubs
						.get(field);
				if (valueSubs != null) {
					FastEventFilter<? extends Event> fastEventFilter = (FastEventFilter<? extends Event>) sub
							.getEventFilter();
					Object value = fastEventFilter.getValue();
					Subscription[] subs = valueSubs.get(value);
					subs = removeFromArray(subs, sub);
					if (subs == null) {
						valueSubs.remove(value);
						if (valueSubs.size() == 0) {
							fastFilterSubs.remove(field);
							if (fastFilterSubs.size() == 0) {
								fastFilterSubs = null;
							}
						}
					} else {
						valueSubs.put(value, subs);
					}
				}
			}
		} else {
			// slow filter
			slowFilterSubs = removeFromArray(slowFilterSubs, sub);
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
