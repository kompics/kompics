package se.sics.kompics;

import java.util.Collection;
import java.util.Map;

public class Unsafe {

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
}
