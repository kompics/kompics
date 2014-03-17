/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.gvod.common.msgs;

import java.util.Calendar;

/**
 *
 * @author jdowling
 */
public class Timestamp implements Comparable<Timestamp> {

	public Timestamp() {
		Calendar cal = Calendar.getInstance();
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day=cal.get(Calendar.DAY_OF_MONTH);
		hour=cal.get(Calendar.HOUR_OF_DAY);
		minute=cal.get(Calendar.MINUTE);
		second=cal.get(Calendar.SECOND);

	}
	public Timestamp(int year, int month, int day, int hour, int minute, int second) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}

	public int year;
	public int month;
	public int day;
	public int hour;
	public int minute;
	public int second;

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof Timestamp)) return false;
			Timestamp objToCompareTo = (Timestamp)obj;
			return compareTo(objToCompareTo)==0;
	}


	public void set(int year, int month, int day, int hour, int minute, int second) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute= minute;
		this.second=second;
	}

	public Timestamp addSeconds(int seconds) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month, day, hour, minute, second);
		cal.add(Calendar.SECOND, seconds);
		return new Timestamp(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Timestamp o) {

		if (this.year>o.year) return 1;
		if (this.year<o.year) return -1;

		if(this.month>o.month) return 1;
		if(this.month<o.month) return -1;

		if(this.day>o.day) return 1;
		if(this.day<o.day) return -1;

		if(this.hour>o.hour) return 1;
		if(this.hour<o.hour) return -1;

		if(this.minute>o.minute) return 1;
		if(this.minute<o.minute) return -1;

		if(this.second>o.second) return 1;
		if(this.second<o.second) return -1;

		return 0;
	}

	/**
	 * @param a timestamp to compare to
	 * @return true if this timestamp is newer than one in param object
	 */
	public boolean newerThan(Timestamp o)
	{
		return compareTo(o)>0;
	}
	/**
	 * @param a timestamp to compare to
	 * @return true if this timestamp is older than one in param object
	 */
	public boolean olderThan(Timestamp o) {
		return compareTo(o)<0;
	}

	public void copy(Timestamp t) {
		year = t.year;
		month = t.month;
		day=t.day;
		hour=t.hour;
		minute=t.minute;
		second=t.second;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.year);
		sb.append(":");
		sb.append(this.month);
		sb.append(":");
		sb.append(this.day);
		sb.append(":");
		sb.append(this.hour);
		sb.append(":");
		sb.append(this.minute);
		sb.append(":");
		sb.append(this.second);
		return sb.toString();
	}
}
