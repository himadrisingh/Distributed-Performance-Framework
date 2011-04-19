/**
 * 
 */
package org.tc.perf.util;


/**
 * @author gautam
 *
 */

/**
 * Utility class for exporting constants. Not meant to be instantiated.
 * 
 */
public class SharedConstants {
    private SharedConstants() {
    }

    public static final String TC_CONFIG_URL = System.getProperty("fw.tc.config", "localhost:8510");

	public static final String HOSTNAME = CommonUtils.getHostname();
	public static final String FILE_SEPARATOR = "/";
	public static final String CLASSPATH_SEPARATOR = (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) ? ";" : ":";

	public final static String KIT = "KIT";
    public final static String TEST_CACHE = "TEST";
    public final static String FILELIST = "CLASSPATH";
    public final static String TC_INSTALL_DIR = "KIT_PATH";
    public final static String CLIENT_SETUP = "/client-setup";
    public final static String NOTIFICATIONS = "NOTIFICATIONS";
    public final static String MAIN_CLASS = "MAIN_CLASS";
    public final static String CONFIG = "CONFIG";

    public static final String TC_MAIN_CLASS = "com.tc.server.TCServerMain";
    public static final String TC_STOP_MAIN_CLASS = "com.tc.admin.TCStop";
    public static final String TC_CLASSPATH = "/lib/tc.jar";
    public static final String TC_START_LOG = "Terracotta Server instance has started up";
    public static final String TC_STOP_LOG = "stopping it";
    

    public static final String SERVER_LOG_LOCATION = "server-" + HOSTNAME + "-logs";
    public static final String CLIENT_LOG_LOCATION  = "client-" + HOSTNAME + "-logs";

}
