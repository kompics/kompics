package se.sics.kompics.core.stats;


public class WorkerStat {

	private static final ThreadLocal<WorkerStat> localStat = new ThreadLocal<WorkerStat>() {
		@Override
		protected WorkerStat initialValue() {
			return new WorkerStat();
		}
	};
	
	int i = 0;
	
	Stats tau = new Stats();
	
	public static WorkerStat get() {
		return localStat.get();
	}
	
	public void tau(long nanos) {
		tau.push(nanos);
		i++;
		
		if (i == 100) {
			i = 0;
			System.err.println(tau.toString());
		}
	}
}
