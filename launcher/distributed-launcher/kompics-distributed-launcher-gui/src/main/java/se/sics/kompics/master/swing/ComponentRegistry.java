/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.master.swing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;

/**
 *
 * @author jdowling
 */
public class ComponentRegistry {

    private static Map<Class<? extends ComponentDefinition>,Map<String,ComponentDefinition>> registry =
            new ConcurrentHashMap<Class<? extends ComponentDefinition>,Map<String,ComponentDefinition>>();

    public static ComponentDefinition registerComponent(Class<? extends ComponentDefinition> cl,
            ComponentDefinition instance, String id)
    {
        Map<String,ComponentDefinition> instances = registry.get(cl);
        if (instances == null) {
            instances = new ConcurrentHashMap<String,ComponentDefinition>();
            registry.put(cl, instances);
        }
        return instances.put(id,instance);
    }

    public static ComponentDefinition getComponent(Class<? extends ComponentDefinition> cl, String id)
    {
        Map<String,ComponentDefinition> components = registry.get(cl);
        if (components != null) {
            return components.get(id);
        }
        else {
            return null;
        }
    }


}
