package se.sics.kompics.wan.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalNetworkConfiguration
{	
	private static final Logger logger = LoggerFactory.getLogger(LocalNetworkConfiguration.class.getName());

	private static String ipAddr = null;
		
	private LocalNetworkConfiguration()
	{
		
	}

	
	public synchronized static String findLocalHostAddress()
	{
		if (ipAddr != null)
		{
			return ipAddr;
		}
		InetAddress inet = findLocalInetAddress();
		if (inet == null)
		{
			throw new IllegalStateException("Could not acquire local network address.");
		//	return "localhost";
		}
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
								logger.warn("Problem with getting IP-address of a network interface: {}", e.getMessage());
							}
							
						}
			}
		}
		catch (SocketException e)
		{
			logger.warn("Couldn't get the local inet address: {}", e.getMessage());
			return null;
		}
		return loopbackAddr;
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

