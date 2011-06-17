/**
 * 
 */
package org.tc.perf.util;

/**
 * Utility class for exporting constants. Not meant to be instantiated.
 * 
 * @author gautam
 */
public class SharedConstants {
	private SharedConstants() {
	}

	public static final int MAX_LENGTH = 64 * 1024; // 64KB
	public static final int WORK_TIME_OUT_MINUTES = 120;

	public static final String TC_CONFIG_URL = System.getProperty(
			"fw.tc.config", "localhost:8510");
	public static final String HOSTNAME = CommonUtils.getHostname();
	public static final String FILE_SEPARATOR = "/";
	public static final String CLASSPATH_SEPARATOR = (System.getProperty(
	"os.name").toLowerCase().indexOf("win") >= 0) ? ";" : ":";

	public final static String KIT_NAME = "KIT_NAME";
	public final static String RUNNING_TESTS = "RUNNING_TESTS";
	public final static String WORK_QUEUE = "WORK_QUEUE";
	public final static String FILELIST = "CLASSPATH";
	public final static String TC_INSTALL_DIR = "KIT_PATH";

}
