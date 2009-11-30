package se.sics.kompics.p2p.experiment.dsl;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeInstrumenter extends ExprEditor {

	private static Logger logger = LoggerFactory
			.getLogger("se.sics.kompics.simulation.CodeInstrumenter");

	private final String s;

	public CodeInstrumenter(String system) {
		if (system != null && !system.equals("")) {
			this.s = system;
		} else {
			this.s = "se.sics.kompics.simulation.SimulatorSystem";
		}
	}

	public CodeInstrumenter() {
		this.s = "se.sics.kompics.simulation.SimulatorSystem";
	}

	// redirect method calls
	@Override
	public void edit(MethodCall m) throws CannotCompileException {
		String callerClassName = m.getFileName();
		String className = m.getClassName();
		String method = m.getMethodName();
		try {
			// this traverses the class hierarchy up
			className = m.getMethod().getDeclaringClass().getName();
		} catch (NotFoundException e) {
			// throw new
			// RuntimeException("Cannot instrument call to "
			// + className + "." + method + "() in "
			// + string, e);

			logger.debug("Cannot instrument call to " + className + "."
					+ method + "() in " + callerClassName + " " + e);
			// e.printStackTrace();
			// return;
		}
		if (className == null || method == null) {
			return;
		}
		// redirect calls to System.currentTimeMillis()
		if (className.equals("java.lang.System")
				&& method.equals("currentTimeMillis")) {

			m.replace("{ $_ = " + s + ".currentTimeMillis(); }");
			return;
		}
		// redirect calls to System.nanoTime()
		if (className.equals("java.lang.System") && method.equals("nanoTime")) {
			m.replace("{ $_ = " + s + ".nanoTime(); }");
			return;
		}
		// redirect calls to Thread.sleep()
		if (className.equals("java.lang.Thread") && method.equals("sleep")) {
			m.replace("{ " + s + ".sleep($$); }");
			return;
		}
		// redirect calls to Thread.start()
		if (className.equals("java.lang.Thread") && method.equals("start")) {
			m.replace("{ " + s + ".start(); }");
			return;
		}

		// make sure the SHA1PRNG is used in SecureRandom
		if (className.equals("java.security.SecureRandom")
				&& method.equals("getPrngAlgorithm")) {
			m.replace("{ $_ = null; }");
			// System.err.println("REPLACED SECURE_RANDOM");
			return;
		}

		// redirect calls to TimeZone.getDefault()
		if (className.equals("java.util.TimeZone")
				&& method.equals("getDefaultRef")) {
			m.replace("{ $_ = " + s + ".getDefaultTimeZone(); }");
			return;
		}
	}
}
