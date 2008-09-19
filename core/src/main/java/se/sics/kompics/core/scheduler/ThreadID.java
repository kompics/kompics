package se.sics.kompics.core.scheduler;

public class ThreadID {

	private static ThreadLocal<Integer> threadID = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return -1;
		}
	};

	public static int get() {
		return threadID.get();
	}

	public static void set(int index) {
		threadID.set(index);
	}
}
