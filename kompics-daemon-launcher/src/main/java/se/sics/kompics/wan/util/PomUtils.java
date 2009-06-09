package se.sics.kompics.wan.util;

import java.io.File;

public class PomUtils {

	
	public static String groupIdToPath(String groupId)
	{
		return groupId.replace('.', File.separatorChar);
	}
	
	public static String sepStr()
	{
		char[] separator = new char[1];
		separator[0] = File.separatorChar;
		return new String(separator);
	}
	
	public static int generateJobId(String groupId, String artifactId, String version)
	{
		int id;
		if ((groupId.hashCode() + artifactId.hashCode() + version.hashCode()) < Integer.MAX_VALUE)
		{
			id = groupId.hashCode() + artifactId.hashCode() + version.hashCode(); 
		}
		else
		{
			id = (groupId.hashCode() + artifactId.hashCode() + version.hashCode()) % Integer.MAX_VALUE;
		}
		
		return id;
	}
	
	
}
