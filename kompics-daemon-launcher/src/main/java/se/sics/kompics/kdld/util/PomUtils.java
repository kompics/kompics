package se.sics.kompics.kdld.util;

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
	
}
