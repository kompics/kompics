package ${package}.main;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

/**
 * The <code>Root</code> class

 */
public final class Root extends ComponentDefinition {

  public static void main(String[] args)
   {
		// This initializes the Kompics runtime, and creates an instance of Root
		Kompics.createAndStart(Root.class);
   }
  
	public Root() {
		subscribe(handleStart, control);
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			// TODO
		}
	};  
}
