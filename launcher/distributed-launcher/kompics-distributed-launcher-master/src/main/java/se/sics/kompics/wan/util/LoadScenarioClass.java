package se.sics.kompics.wan.util;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;


/**
 * Used by a GUI to load and execte SimulationScenario java file experiments.
 * 
 * @author jdowling
 * 
 */
public class LoadScenarioClass {

	private final static String CLASSES_DIR = "/target/classes/";
	/**
	 * @param classFilename
	 *            filename.java for the SimulationScenario class
	 * @param out
	 *            prints detailed error messages here.
	 * @return error code
	 */
	private static int compileScenarioClassfile(String classFilename, PrintWriter out) {
		String classPath = System.getProperty("java.class.path");
		
		// XXX detect source code change since last compilation
		
//		return com.sun.tools.javac.Main.compile(new String[] { "-classpath", classPath, "-d",
//				CLASSES_DIR, classFilename }, out); // "dynacode/sample/PostmanImpl.java"
		return 0;
	}

	// The basic idea is to load the dynamic class using our own URLClassLoader.
	// Whenever the source file is changed and recompiled, we discard the old
	// class
	// (for garbage collection later) and create a new URLClassLoader to load
	// the class again.
	public static SimulationScenario 
		reloadClass(String className, String classFilename, PrintWriter out) {
		
		// compile the class first (on-demand)
		if (compileScenarioClassfile(classFilename, out) != 0)
		{
			return null;
		}
		ClassLoader parentLoader = SimulationScenario.class.getClassLoader();
		 
		SimulationScenario scenario = null;

		// The dir contains the compiled classes.
		File classesDir = new File(CLASSES_DIR);

		URLClassLoader loader1 = null;
		try {
			loader1 = new URLClassLoader(new URL[] { classesDir.toURL() }, parentLoader);
			Class<?> cls1 = null;
			cls1 = loader1.loadClass(className);
			scenario = (SimulationScenario) cls1.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return scenario;

	}
}
