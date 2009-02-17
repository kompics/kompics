package se.sics.kompics.manual.example.composition;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;

public class Main extends ComponentDefinition {

	Component outer;
	public static void main(String[] args) {
		Kompics.createAndStart(Main.class);
	}
	
	public Main() {
		outer = create(Outer.class);
		trigger(new TestEvent(), outer.getPositive(Port.class));
	}

}
