package se.sics.kompics.example;

import java.util.Properties;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentStopMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;

@ComponentType
public class UserComponent {

	private final Component component;

	public UserComponent(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		System.out.println("CREATE USER");
	}

	@ComponentInitializeMethod("user.properties")
	public void initialize(Properties properties) {
		System.out.println("INITIALIZE USER " + properties.getProperty("name"));
	}

	@ComponentDestroyMethod
	public void destroy() {
		System.out.println("DESTROY USER");
	}

	@ComponentStartMethod
	public void start() {
		System.out.println("START USER");
	}

	@ComponentStopMethod
	public void stop() {
		System.out.println("STOP USER");
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { HelloEvent.class })
	public void handleInputEvent(InputEvent event) {
		System.out.println("HANDLE INPUT in USER");
		component.triggerEvent(new HelloEvent("Hello"));
		System.out.println("USER TRIGGERED HELLO: I said Hello to the World");
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { OutputEvent.class })
	public void handleResponseEvent(ResponseEvent event) {
		System.out.println("HANDLE RESPONSE in USER");
		String message = event.getMessage();
		System.out.println("USER: I got message: \"" + message + "\"");
		System.out.println("TRIGGER OUTPUT in USER");
		component.triggerEvent(new OutputEvent());
		int x = 0;
		x = 1 / x;
	}
}
