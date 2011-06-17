/**
 * 
 */
package org.tc.perf.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

/**
 * Class with simple utility methods
 * 
 * @author Himadri Singh
 * 
 */
public class CommonUtils {
	private CommonUtils() {
	} // not meant to be instantiated

	/**
	 * returns the hostname of the machine.
	 * 
	 * @return hostname
	 */

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

	/**
	 * deletes the directory
	 * 
	 * @param location
	 *            directory path
	 * @throws IOException
	 */
	public static void deleteDir(String location) throws IOException {
		File dirs = new File(location);
		FileUtils.deleteDirectory(dirs);
	}

	/**
	 * Sleeps the thread
	 * 
	 * @param millis
	 *            time in milliseconds
	 */
	public static void sleep(long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
