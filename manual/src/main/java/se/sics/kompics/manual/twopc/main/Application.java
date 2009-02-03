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
package se.sics.kompics.manual.twopc.main;

import javax.swing.UnsupportedLookAndFeelException;

import se.sics.kompics.launch.Scenario;
import se.sics.kompics.launch.Topology;

/**
 * The <code>TwoPhaseCommitExecutor</code> class.
 * 
 * @author Jim Dowling 
 */
@SuppressWarnings("serial")
public final class Application {

	/**
	 * The main method.
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
				link(1, 2, 3000, 0.5).bidirectional();
			}
		};

		Topology topology2 = new Topology() {
			{
				node(1, "127.0.0.1", 22031);
				node(2, "127.0.0.1", 22032);
				node(3, "127.0.0.1", 22033);
				node(4, "127.0.0.1", 22034);

				link(1, 2, 3000, 0.5).bidirectional();
				link(1, 3, 3000, 0.5).bidirectional();
				link(1, 4, 3000, 0.5).bidirectional();
//				 defaultLinks(1000, 0);
			}
		};

		Scenario scenario1 = new Scenario(ApplicationGroup.class) {
			{
				command(1, "T5:X"); //.recover("Prrr", 1000);
			}
		};

		Scenario scenario2 = new Scenario(ApplicationGroup.class) {
			{
				command(1, "S500:T9:S300:T1:X"); //.recover(1000).recover("S500:Pok", 1000);
//				command(2, "S500:Pb2:S300:LB2");
//				command(3, "S500:Lc3:S300:PC3");
//				command(4, "S500:Pd4:S300:LD4");
			}
		};


		scenario1.executeOn(topology1);
		scenario1.executeOn(topology2);
		scenario2.executeOn(topology2);

		System.exit(0);
	}
}
