package org.tc.cluster.watcher.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;


public class ClusterWatcherProperties {

	private static final Logger LOG	= Logger.getLogger(ClusterWatcherProperties.class);
	private static final Properties props = new Properties();

	public static int LOW_TXR_THRESHOLD;
	public static long PROBE_INTERVAL;
	public static long ONE_ACTIVE_MAX_CHECK;
	public static long MISSING_CLIENT_MAX_CHECK;
	public static long LOW_TXR_MAX_CHECK;
	public static long CLUSTER_DOWN_MAX_CHECK;
	public static long CLIENT_COUNTS;
	public static String TC_CONFIG;
	public static boolean CHECK_CLUSTER_DOWN;

	private static void printProps(){
		LOG.info("ClusterWatcher Properties - " +
				"\n\ttc-config url: " + TC_CONFIG +
				"\n\tClient Count: " + CLIENT_COUNTS +
				"\n\tProbe interval: " + PROBE_INTERVAL + " ms" +
				"\n\tOne Active Server Max Check: " + ONE_ACTIVE_MAX_CHECK +
				"\n\tMissing Clients Max Check: " + MISSING_CLIENT_MAX_CHECK +
				"\n\tCluster Down Max Check: " + CLUSTER_DOWN_MAX_CHECK	 +
				"\n\tLow Txn Max Check: " + LOW_TXR_MAX_CHECK +
				"\n\tLow Txn Threshold: " + LOW_TXR_THRESHOLD);
	}

	private static void refresh(){
		LOW_TXR_THRESHOLD        	= getPropertyAsInt("low.txr.threshold" , 5);
		PROBE_INTERVAL           	= toMillis(getProperty("probe.interval" , "5s"));
		ONE_ACTIVE_MAX_CHECK     	= getPropertyAsInt("one.active.max.check" , 24);
		MISSING_CLIENT_MAX_CHECK 	= getPropertyAsInt("missing.client.max.check" , 24);
		LOW_TXR_MAX_CHECK        	= getPropertyAsInt("low.txr.max.check" , 24);
		CLUSTER_DOWN_MAX_CHECK    	= getPropertyAsInt("cluster.down.max.check" , 24);
		CLIENT_COUNTS 				= getPropertyAsInt("clientcount" , 0);
		TC_CONFIG					= getProperty("tc-config.url","localhost:9510");
		CHECK_CLUSTER_DOWN			= getBoolean("cluster.down.check");
	}

	public static void loadProperties(String propertyFile){
		try {
			props.clear();
			URL resUrl = new URL("file:" + propertyFile);
			props.load(resUrl.openStream());
			//      props.list(System.out);
			props.putAll(System.getProperties());
			refresh();
			printProps();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void loadProperties(Properties p){
		props.clear();
		props.putAll(p);
		refresh();
		LOG.info(props);
		LOG.info(p);
		printProps();
	}

	public static String getProperty(String key) {
		return props.getProperty(key);
	}

	public static String getProperty(String key, String defaultVal) {
		return props.getProperty(key, defaultVal);
	}

	public static int getPropertyAsInt(String name, int defaultVal) {
		return Integer.parseInt(getProperty(name, String.valueOf(defaultVal)).trim());
	}

	public static double getPropertyAsDouble(String name, double defaultVal) {
		return Double.parseDouble(getProperty(name, String.valueOf(defaultVal)).trim());
	}

	public static long getPropertyAsLong(String name, long defaultVal) {
		return Long.parseLong(getProperty(name, String.valueOf(defaultVal)).trim());
	}

	public static boolean getBoolean(String key) {
		String value = getProperty(key);
		if (value != null) {
			return Boolean.valueOf(value);
		}
		return false;
	}

	public static long toMillis(String time) {
		String[] parts = time.trim().split(":");
		long millis = 0L;
		for (String part : parts) {
			String value = part.substring(0, part.length() - 1).trim();
			if (part.endsWith("h")) {
				millis += Integer.valueOf(value) * 60 * 60 * 1000L;
			} else if (part.endsWith("m")) {
				millis += Integer.valueOf(value) * 60 * 1000L;
			} else if (part.endsWith("s")) {
				millis += Integer.valueOf(value) * 1000L;
			} else {
				millis += Integer.valueOf(part);
			}
		}
		return millis;
	}
}
