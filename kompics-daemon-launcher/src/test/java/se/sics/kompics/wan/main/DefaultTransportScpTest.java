package se.sics.kompics.wan.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import se.sics.kompics.wan.util.Transporter;

public class DefaultTransportScpTest extends PlexusTestCase {
	
	private Transporter transporter = null;
	
	public void setUp() {
		Exception e = null;
	    try {        
			super.setUp();
			
			transporter = (Transporter)  lookup(Transporter.ROLE, "default");
			
		} catch (Exception e1) {
			System.out.println(e1.getMessage());
			e = e1;
		}    
		assertNull(e);
		assertNotNull(transporter);    
	}
	
	public void testURLWithoutSpace() {
		
		Exception e = null;
		File tmp = null;
		int read = 0;
		
		try {
			tmp = FileUtils.createTempFile("ambiance-transporter-", ".test", null);
//			transporter.get("scp://lgranie:po09iu87@svn/home/lgranie/derby.log", tmp);
			transporter.get("scp://jdowling:password@lqist.com:/home/jdowling/blah.log", tmp);
			FileInputStream fis = new FileInputStream(tmp);
			while(fis.read() != -1) {
				read++;
			}
		} catch (Exception e1) {
			e = e1;
			e1.printStackTrace();
		}
		
		assertNull(e);
		assertNotNull(tmp);
//		assertEquals(read, 37);
	}
	
	public void testURLWithSpace() {
		
		Exception e = null;
		File tmp = null;
		int read = 0;
		
		try {
			tmp = FileUtils.createTempFile("ambiance-transporter-", ".test", null);
			transporter.get("http://lucan.sics.se/name with space.txt", tmp);
			FileInputStream fis = new FileInputStream(tmp);
			while(fis.read() != -1) {
				read++;
			}
		} catch (Exception e1) {
			e = e1;
			e1.printStackTrace();
		}
		
		assertNull(e);
		assertNotNull(tmp);
		assertEquals(read, 31);
	}
	
	public void testURLWithoutSpaceAsStream() {
		
		Exception e = null;
		InputStream is = null;
		int read = 0;
		try {
			String url = "http://lucan.sics.se/name_without_space.txt";
			is = transporter.getAsStream(url);
			while(is.read() != -1) {
				read++;
			}
		} catch (Exception e1) {
			e = e1;
			e1.printStackTrace();
		}
		
		assertNull(e);
		assertNotNull(is);
		assertEquals(read, 37);
	}
	
	public void testURLWithSpaceAsStream() {
		
		Exception e = null;
		InputStream is = null;
		int read = 0;
		try {
			String url = "http://lucan.sics.se/name with space.txt";
			is = transporter.getAsStream(url);
			while(is.read() != -1) {
				read++;
			}
		} catch (Exception e1) {
			e = e1;
			e1.printStackTrace();
		}
		
		assertNull(e);
		assertNotNull(is);
		assertEquals(read, 31);
	}	
	
}