package test;

public class DemoComponent implements DemoPort.Provided {

	DemoPort.Required usedDemo;

	@Override
	public void e1() {
		usedDemo.e2();
	}

	@Override
	public void e2() {
	}
}

// TODO 1: subtype subscription
// TODO 2: blocking calls

interface Network {
	public interface Provided {
		void send();
	}

	public interface Required {
		void deliver(int a, int d);
	}

}

class Link implements Network.Required {
	@Override
	public void deliver(int arg0, int arg1) {
		// TODO Auto-generated method stub

	}
}
