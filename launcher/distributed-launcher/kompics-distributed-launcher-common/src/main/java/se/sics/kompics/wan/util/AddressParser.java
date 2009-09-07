package se.sics.kompics.wan.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.config.Configuration;

/**
 * The <code>HostsParser</code> class.
 * 
 * @author Jim Dowling 
 */
public final class AddressParser {
	private static final Logger logger = LoggerFactory.getLogger(AddressParser.class);

	/**
	 * If a hostname from the list is not resolved, a warning is printed (no exception is thrown).
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws AddressParserException
	 */
	public static TreeSet<Address> parseAddressesFile(String fileName) throws FileNotFoundException, AddressParserException
	{
		TreeSet<Address> addrs = new TreeSet<Address>();
		FileInputStream hostFile = new FileInputStream(fileName);
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(hostFile));
		if (in == null)
		{
			return null;
		}
		String hostPortIdPerLine = "";
		while (hostPortIdPerLine != null) {
			try {
				hostPortIdPerLine = in.readLine();
				if (hostPortIdPerLine != null)
				{
					String[] hosts = hostPortIdPerLine.split(",");
					for (String h : hosts) {
						try {
							Address addr = parseAddress(h);
							if (addr != null) {
								addrs.add(addr);
							}
						}
						catch (UnknownHostException e)
						{
							logger.warn("Unknown host:" + e.getMessage());
						}
					}
				}
			} catch (NumberFormatException e) {
				throw new AddressParserException("Invalid port or id number: " + e.getMessage());
			}
			catch (IOException e) {
				throw new AddressParserException(e.getMessage());
			}
		
		}
		try {
			in.close();
		} catch (IOException e) {
			throw new AddressParserException(e.getMessage());
		}
		
		return addrs;
	}
	
	public static Address parseAddress(String h) throws UnknownHostException
	{
		String[] idParts = h.split("@");

                int addressOffset = 0;
                if (idParts.length == 2) {
                    addressOffset = 1;
                }
                String[] addressParts = idParts[addressOffset].split(":");
		InetAddress host = null;
                // Works for both hostnames and textual format IP addrs.
                host = InetAddress.getByName(addressParts[0]);
		int port = Configuration.DEFAULT_MASTER_PORT;
		int id =  Configuration.DEFAULT_DAEMON_ID;
		if (addressParts.length >= 2)
		{
			port =  Integer.parseInt(addressParts[1]);
		}
		if (idParts.length >= 2)
		{
			id =  Integer.parseInt(idParts[0]);
		}
		Address addr = new Address(host, port, id);

		return addr;

	}

}
