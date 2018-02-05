package se.sics.kompics;

import java.util.Collection;
import java.util.Map;

public abstract class Unsafe {

  public static Map<Class<? extends PortType>, JavaPort<? extends PortType>> getPositivePorts(Component component) {
       return ((JavaComponent) component).getPositivePorts();
  }

  public static Map<Class<? extends PortType>, JavaPort<? extends PortType>> getNegativePorts(Component component) {
       return ((JavaComponent) component).getNegativePorts();
  }

  public static Collection<Class<? extends KompicsEvent>> getPositiveEvents(PortType portType) {
       return portType.getPositiveEvents();
  }

  public static Collection<Class<? extends KompicsEvent>> getNegativeEvents(PortType portType) {
       return portType.getNegativeEvents();
  }

  public static void setOrigin(Direct.Request<? extends Direct.Response> request, Port origin) {
       request.origin = origin;
  }

  public static Port getOrigin(Direct.Request<? extends Direct.Response> request) {
       return request.getOrigin();
  }

  public static <P extends PortType> JavaPort<P> createJavaPort(boolean positive, P portType, ComponentCore owner) {
       return new JavaPort<P>(positive, portType, owner, owner.tracer);
  }
}
