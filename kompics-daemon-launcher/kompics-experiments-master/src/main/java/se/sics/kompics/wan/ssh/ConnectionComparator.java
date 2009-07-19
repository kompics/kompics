package se.sics.kompics.wan.ssh;

import java.io.IOException;
import java.util.Comparator;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;

/**
 * The <code>ConnectionComparator</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ConnectionComparator implements Comparator<Connection> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Connection arg0, Connection arg1) {
		
		if ((arg0.getHostname().compareTo(arg1.getHostname()) != 0) || 
				(arg0.getPort() != arg1.getPort())) {
			return -1;
		}
		
		ConnectionInfo c1 = null;
		try {
			c1 = arg0.getConnectionInfo();
		} catch (IOException e1) {
			// do nothing
		} 
		
		ConnectionInfo c2 = null;
		try {
			c2 = arg1.getConnectionInfo();
		} catch (IOException e1) {
			// do nothing
			if (c1 == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		if (c1 == null) {
			return -1;
		}
		
			if ((c1.keyExchangeAlgorithm.compareTo(c2.keyExchangeAlgorithm) != 0)
					|| (c1.keyExchangeCounter != c2.keyExchangeCounter))
			{
				return -1;
			}
		
		return 0;
	}

}
