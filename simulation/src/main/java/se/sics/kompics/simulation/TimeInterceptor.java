/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.Translator;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * The <code>TimeInterceptor</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class TimeInterceptor implements Translator {

	private static final String s = "se.sics.kompics.simulation.SimulatorSystem";

	private static HashSet<String> exceptions = new HashSet<String>();

	public void start(ClassPool pool) throws NotFoundException,
			CannotCompileException {

		// well known exceptions
		exceptions.add("se.sics.kompics.p2p.simulator.P2pSimulator");
		exceptions.add("org.apache.log4j.PropertyConfigurator");
		exceptions.add("org.apache.log4j.helpers.FileWatchdog");
		exceptions.add("org.mortbay.thread.QueuedThreadPool");
		exceptions.add("org.mortbay.io.nio.SelectorManager");
		exceptions.add("org.mortbay.io.nio.SelectorManager$SelectSet");
		exceptions.add("org.apache.commons.math.stat.descriptive.SummaryStatistics");
		exceptions.add("org.apache.commons.math.stat.descriptive.DescriptiveStatistics");

		// try to add user-defined exceptions from properties file
		InputStream in = ClassLoader
				.getSystemResourceAsStream("timer.interceptor.properties");
		Properties p = new Properties();
		if (in != null) {
			try {
				p.load(in);
				for (String classname : p.stringPropertyNames()) {
					String value = p.getProperty(classname);
					if (value != null && value.equals("IGNORE")) {
						exceptions.add(classname);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onLoad(ClassPool pool, final String classname)
			throws NotFoundException, CannotCompileException {

		int d = classname.indexOf("$");
		String outerClass = (d == -1 ? classname : classname.substring(0, d));

		if (exceptions.contains(outerClass)) {
			return;
		}

		CtClass cc = pool.get(classname);

		// makeSerializable(pool, cc);

		cc.defrost();
		cc.instrument(new ExprEditor() {
			// redirect method calls
			@Override
			public void edit(MethodCall m) throws CannotCompileException {
				String className = m.getClassName();
				String method = m.getMethodName();
				try {
					className = m.getMethod().getDeclaringClass().getName();
				} catch (NotFoundException e) {
					throw new RuntimeException("Cannot instrument call to "
							+ className + "." + method + "() in " + classname,
							e);
				}
				if (className == null || method == null) {
					return;
				}
				// redirect calls to System.currentTimeMillis()
				if (className.equals("java.lang.System")
						&& method.equals("currentTimeMillis")) {

					m.replace("{ $_ = " + s + ".currentTimeMillis(); }");
				}
				// redirect calls to System.nanoTime()
				if (className.equals("java.lang.System")
						&& method.equals("nanoTime")) {
					m.replace("{ $_ = " + s + ".nanoTime(); }");
				}
				// redirect calls to Thread.sleep()
				if (className.equals("java.lang.Thread")
						&& method.equals("sleep")) {
					m.replace("{ " + s + ".sleep($$); }");
				}
				// redirect calls to Thread.start()
				if (className.equals("java.lang.Thread")
						&& method.equals("start")) {
					m.replace("{ " + s + ".start(); }");
				}
				// redirect calls to Random.next*()
				// if (className.equals("java.util.Random")
				// && method.startsWith("next")) {
				// m.replace("{ $_ = " + s + ".random(); }");
				// }
			}

			// @Override
			// public void edit(NewExpr e) throws CannotCompileException {
			// String className = e.getClassName();
			// try {
			// if (className.equals("java.util.Random")) {
			// System.err.println("FOUND RAND in " +
			// e.getEnclosingClass().getName());
			// }
			// CtConstructor constructor = e.getConstructor();
			// // constructor.
			// } catch (NotFoundException e1) {
			// throw new RuntimeException("Cannot instrument constructor "
			// + className + "()", e1);
			// }
			// }
		});
	}

	// private void makeSerializable(ClassPool pool, CtClass cc)
	// throws NotFoundException {
	// boolean alreadySerializable = false;
	// CtClass parent = cc;
	//		
	// // abort if class is already Serializable
	// do {
	// CtClass[] interfaces = parent.getInterfaces();
	// for (int i = 0; i < interfaces.length; i++) {
	// if (interfaces[i].getName().equals("java.io.Serializable")) {
	// alreadySerializable = true;
	// }
	// }
	// parent = parent.getSuperclass();
	// } while (parent != null);
	//
	// if (!alreadySerializable) {
	// cc.addInterface(pool.get("java.io.Serializable"));
	// }
	// }
}