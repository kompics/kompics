package ${package}.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

/**
 * The <code>Root</code> class

 */
public final class Root extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
	.getLogger(Root.class);

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
			logger.info("SUCCESS: Root started");
			logger.info("Press 'crl-c' to exit.");
		}
	};  
}
