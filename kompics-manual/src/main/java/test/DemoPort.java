package test;

public interface DemoPort {

	public interface Provided {
		void e1();

		void e2();
	}

	public interface Required {
		void e2();

		void e3();
	}
}
