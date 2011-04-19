package org.tc.perf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Configuration implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Configuration.class);

	private final Properties props;
	private final String kitLocation;
	private final List<File> testDirectories;
	private final List<String> classpathRegex;
	private final String mainClass;
	private final String location;
	private final List<String> arguments;
	private final List<String> l1_jvmArgs;
	private final List<String> l2_jvmArgs;

	private final List<String> l2machines;
	private final List<String> l1machines;
	private final List<String> loadmachines;

	private final boolean dgcEnabled;
	private final int dgcInterval;
	private final String  persistence;
	//	private final String dataShareMode;
	private final boolean offheapEnabled;
	private final String offheapMaxDataSize;
	private final String licenseFileLocation;

	public Configuration(final String properties) {
		props = loadProperties(properties);

		kitLocation = getRequiredString("kit.location");
		licenseFileLocation = getString("kit.licenseLocation",
		"target/terracotta-license.key");
		testDirectories = getDirectories(toList(getString("directories","target/ target/dependencies")));
		classpathRegex = toList(getString("classpath", "*.jar *.xml *.properties"));
		mainClass = getRequiredString("main-classname");
		location = getString("logLocation", "target/");
		arguments = toList(getString("arguments", ""));

		l2machines = toList(getRequiredString("l2machines").toLowerCase());
		l1machines = toList(getRequiredString("l1machines").toLowerCase());
		loadmachines = toList(getString("loadmachines","").toLowerCase());

		l1_jvmArgs = toList(getString("l1_jvm_args", ""));
		l2_jvmArgs = toList(getString("l2_jvm_args", ""));

		dgcEnabled = getBoolean("dgc.enabled", true);
		dgcInterval = getInteger("dgc.interval", 300);
		persistence = (getBoolean("persistence.enabled", false))?"permanent-store":"temporary-swap-only";

		offheapEnabled = getBoolean("l2.offheap.enabled", false);
		offheapMaxDataSize = getString("l2.offheap.maxDataSize", "1g");
	}

	private List<File> getDirectories(final List<String> dirNames) {
		List<File> directories = new ArrayList<File>();
		for (String dir : dirNames) {
			File d = new File(dir);
			if (d.isDirectory()) {
				log.info("Test directories found: " + d.getAbsolutePath());
				directories.add(d);
			} else
				log.warn("Can't find directory: " + d.getAbsolutePath() + " : "
						+ dir);
		}

		if (directories.size() == 0)
			throw new RuntimeException("Test jar directories cant be zero.");

		return directories;
	}

	public Properties getProps() {
		return props;
	}

	public String getKitLocation() {
		return kitLocation;
	}

	/**
	 * Get the location of the license file on the system
	 * 
	 * @return the licenseFileLocation
	 */
	public String getLicenseFileLocation() {
		return licenseFileLocation;
	}

	public List<File> getTestDirectories() {
		return testDirectories;
	}

	public List<String> getClasspathRegex() {
		return classpathRegex;
	}

	public String getMainClass() {
		return mainClass;
	}

	public String getLocation() {
		return location;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public List<String> getL2machines() {
		return l2machines;
	}

	public List<String> getL1machines() {
		return l1machines;
	}

	public List<String> getLoadmachines() {
		return loadmachines;
	}

	public boolean isDgcEnabled() {
		return dgcEnabled;
	}

	public int getDgcInterval() {
		return dgcInterval;
	}

	public String getPersistence() {
		return persistence;
	}

	public boolean isOffheapEnabled() {
		return offheapEnabled;
	}

	public String getOffheapMaxDataSize() {
		return offheapMaxDataSize;
	}

	public List<String> getL1_jvmArgs() {
		return l1_jvmArgs;
	}

	public List<String> getL2_jvmArgs() {
		return l2_jvmArgs;
	}

	public List<String> toList(final String value){
		// If the string is null or empty, return an empty array.
		if (value == null || value.trim().length() == 0)
			return new ArrayList<String>();
		else
			return Arrays.asList(value.split(" "));
	}

	public Boolean getBoolean(final String key, final boolean defaultValue) {
		return Boolean.valueOf(getString(key, String.valueOf(defaultValue)));
	}

	public Long getLong(final String key, final long defaultValue) {
		return Long.valueOf(getString(key, String.valueOf(defaultValue)));
	}

	public Integer getInteger(final String key, final int defaultValue) {
		return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
	}

	public String getString(final String key, final String defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			log.warn("Key not found in Properties: " + key
					+ " , Using defaults: " + defaultValue);
			props.setProperty(key, defaultValue);
			return defaultValue;
		}
		return value.trim();
	}

	public String getRequiredString(final String key) {
		String prop = props.getProperty(key);
		if (prop == null) {
			log.fatal("Required property not found: " + key);
			System.exit(1);
		}
		return prop.trim();
	}

	private static Properties loadProperties(final String location) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(location));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find properties file.",e);
		}
		return props;
	}

}
