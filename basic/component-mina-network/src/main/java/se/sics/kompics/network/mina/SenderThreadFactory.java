package se.sics.kompics.network.mina;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SenderThreadFactory implements ThreadFactory {
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final String namePrefix;

	SenderThreadFactory() {
		namePrefix = "MINA-sender-";
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}
