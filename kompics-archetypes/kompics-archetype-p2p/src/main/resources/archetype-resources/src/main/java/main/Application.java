/**
 * This file is part of the ID2203 course assignments kit.
 * 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package ${package}.main;

import javax.swing.UnsupportedLookAndFeelException;

import se.sics.kompics.launch.Scenario;
import se.sics.kompics.launch.Topology;

/**
 * The <code>Application</code> class.
 * 
 * @author Jim Dowling 
 */
@SuppressWarnings("serial")
public final class Application {

	/**
	 * The main method.
	 * Execute using:
	 * mvn exec:java -Dexec.mainClass=[package].main.Application
	 * 
	 * @param args
	 *            the arguments
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static final void main(String[] args) {

		Topology topology1 = new Topology() {
			{
				node(1, "127.0.0.1", 22031);
				node(2, "127.0.0.1", 22032);
				defaultLinks(1000,0);
			}
		};

		Scenario scenario1 = new Scenario(ApplicationGroup.class) {
			{
				command(1, "S1000:H:S10000:X"); // 
			}
		};

		scenario1.executeOn(topology1);
		System.exit(0);
	}
}
