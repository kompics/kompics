/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics;

import se.sics.gvod.net.events.PortAllocRequest;
import se.sics.gvod.net.events.PortAllocResponse;


/**
 *
 * @author jdowling
 */
public class MyPortAllocResponse  extends PortAllocResponse
{
    public MyPortAllocResponse(PortAllocRequest request, Object key) {
        super(request,key);
    }
}
