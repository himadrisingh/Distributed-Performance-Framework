/**
 * 
 */
package org.tc.perf.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class with simple utility methods
 * 
 */
public class CommonUtils {
	private CommonUtils() {
	} // not meant to be instantiated

	public static String getHostname() {
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return (localhost != null) ? localhost.getHostName().toLowerCase()
				: "UNKNOWN";
	}
}
