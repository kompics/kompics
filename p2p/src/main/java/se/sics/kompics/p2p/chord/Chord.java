package se.sics.kompics.p2p.chord;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;

/**
 * The <code>Chord</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Chord {

	private final Component component;

	// Chord channels
	private Channel requestChannel, notificationChannel;

	public Chord(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel notificationChannel) {
		;
	}
}
