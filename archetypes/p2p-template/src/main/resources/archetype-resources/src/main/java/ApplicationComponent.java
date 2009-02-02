package se.sics.kompics;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;


/**
 * The <code>ApplicationComponent</code> class
 * 
 * @author Cosmin Arad
 * @author Jim Dowling
 */
@ComponentSpecification
public final class ApplicationComponent {


	private Component component;

	// timer channels
	private Channel timerSetChannel, timerSignalChannel;

	private ComponentMembrane consensusPortMembrane;

	public ApplicationComponent(Component component) {
		this.component = component;

	}

	@ComponentCreateMethod
	public void create(Channel startChannel) {

	}

	@ComponentInitializeMethod
	public void init() {
	}


}

