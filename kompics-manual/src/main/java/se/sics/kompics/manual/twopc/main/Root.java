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
public final class Root {

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
//				node(2, "127.0.0.1", 22033);
//				node(3, "127.0.0.1", 22034);
//				link(1, 2, 3000, 0.5).bidirectional();
//				link(1, 2, 3000, 0.5).bidirectional();
				defaultLinks(1000,0);
			}
		};


		Scenario scenario1 = new Scenario(RootPerProcess.class) {
			{
				// Need to do a sleep to ensure that process '2' has started before 
				// process '1' sends messages to it.
				command(1, "S1000:B:Wjim,dowling:Rjim:Wlinda,gronqvist:Rjim:C"); // Rjim:  
				command(2, "");
//				command(3, ""); 
//				command(4, ""); 
			}
		};


		scenario1.executeOn(topology1);

		System.exit(0);
	}
}
