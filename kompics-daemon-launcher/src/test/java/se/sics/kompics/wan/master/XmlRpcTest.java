package se.sics.kompics.wan.master;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class XmlRpcTest { 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new XmlRpcTest(args[0]);
	}

	@SuppressWarnings("unchecked")
	public XmlRpcTest(String myPlcPassWord) {

		try {

			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

			config.setServerURL(new URL("https://www.planet-lab.org/PLCAPI/"));
			config.setEncoding("iso-8859-1");
			config.setEnabledForExtensions(true);
//			config.setBasicUserName("jdowling@sics.se");
			// config.setBasicPassword(myPlcPassWord);
			XmlRpcClient client = new XmlRpcClient();
			//client.setTypeFactory(new MyTypeFactory(client));

			client.setConfig(config);
			Hashtable auth = new Hashtable();
			auth.put("Username", "kost@sics.se");
			auth.put("AuthMethod", "password");
			auth.put("AuthString", myPlcPassWord);
			auth.put("Role", "user");

			Vector nodes = new Vector();
			Vector fields = new Vector();
			fields.add("hostname");
			fields.add("boot_state");

			Vector params = new Vector();
			params.add(auth);
			params.add(nodes);
			params.add(fields);

			//			
			// Object[] params;
			Object result = client.execute("AdmGetNodes", params);

			Object[] res = (Object[]) result;
			for (int i = 0; i < res.length; i++) {
				HashMap hash = (HashMap) res[i];
				System.out.println("Hostname: " + hash.get("hostname")
						+ "\tBootstate: " + hash.get("boot_state"));
			}

		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
