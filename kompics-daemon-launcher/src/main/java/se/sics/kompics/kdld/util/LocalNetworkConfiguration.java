package se.sics.kompics.kdld.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;

public class LocalNetworkConfiguration
{	
	private static final Logger logger = LoggerFactory.getLogger(LocalNetworkConfiguration.class.getName());

	private static int PORT = 7409;
	
	private static Set<Integer> ALLOCATED_IDS = new HashSet<Integer>();
		
	private LocalNetworkConfiguration()
	{
		
	}

	
	public synchronized static String findLocalHostAddress()
	{
		InetAddress inet = findLocalInetAddress();
		return inet.getHostAddress();		
	}
	
	public synchronized static InetAddress findLocalInetAddress()
	{
		// generate hashId from MAC-addressgetLocalInetAddrP
		Enumeration<NetworkInterface> nis =null;
		InetAddress loopbackAddr = null;
	
		try
		{
			nis = NetworkInterface.getNetworkInterfaces();
			while( nis.hasMoreElements())
			{	
				NetworkInterface ni = (NetworkInterface)nis.nextElement();
						Enumeration<InetAddress> listInetAddr = ni.getInetAddresses();
						while (listInetAddr.hasMoreElements())
						{
							InetAddress addr = listInetAddr.nextElement();
							// return first IP address found that is not local and reachable
							try
							{
								if (addr.isLoopbackAddress() == false  && 
										((addr instanceof Inet6Address) == false))
								{
									return addr;
								}
							}
							catch (Exception e)
							{
								logger.warn("Problem with getting IP-address of a network interface.");
							}
							
						}
			}
		}
		catch (SocketException e)
		{
			logger.debug("Couldn't get the local inet address");
			return null;
		}
		return loopbackAddr;
	}
	
	/**
	 * gets the local PORT.
	 * If this method is called for the first time, a random PORT is generated
	 * between {@link #LOWER_BOUND_PORT_RANGE} 
	 * and {@link #UPPER_BOUND_PORT_RANGE} 
	 * 
	 * @return a PORT number.
	 * @see LocalNetworkConfiguration#LOWER_BOUND_PORT_RANGE
	 * @see LocalNetworkConfiguration#UPPER_BOUND_PORT_RANGE
	 */
	public static int getPort()
	{
		return PORT; 
	}
	
	public static synchronized Address getAddress(int id)
	{
		InetAddress inet = findLocalInetAddress();
		if (inet == null)
		{
			throw new IllegalStateException("Local network address not initialized correctly!");
		}
		if (ALLOCATED_IDS.contains(id))
		{
			throw new IllegalStateException("The ID for the requested peer has already been allocated!");			
		}
		ALLOCATED_IDS.add(id);
		
		Address addr = new Address(inet, PORT, id);
		
		return addr;
		
	}

	private static double getIpAsDouble()
	{
        byte[] bytes = findLocalInetAddress().getAddress();
        
        return 16777216 * (bytes[0] & 0xFF) + 65536 * (bytes[1] & 0xFF) + 256
            * (bytes[2] & 0xFF) + (bytes[3] & 0xFF);
	}
	
	@SuppressWarnings("unused")
	private static byte[] convertIpFromIntToByte(int ipAsInt) throws UnknownHostException
	{
		 byte[] dword = new byte[4];
	        dword[3] = (byte) (ipAsInt & 0x00FF);
	        dword[2] = (byte) ((ipAsInt >> 8) & 0x000000FF);
	        dword[1] = (byte) ((ipAsInt >> 16) & 0x000000FF);
	        dword[0] = (byte) ((ipAsInt >> 24) & 0x000000FF);
	 	 
	        return dword;
	}

}

