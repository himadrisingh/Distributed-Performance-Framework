package org.tc.perf.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * This class is used to define the arguments needed to start a java process.
 * 
 * @author Himadri Singh
 */
public class ProcessConfig {

	private final List<String> defaultArgs = Arrays.asList(
			"-XX:+HeapDumpOnOutOfMemoryError", "-verbose:gc",
			"-XX:+PrintGCTimeStamps", "-XX:+PrintGCDetails",
			"-Dcom.sun.management.jmxremote", "-showversion",
			"-Dcom.sun.management.jmxremote.ssl=false",
	"-Dcom.sun.management.jmxremote.authenticate=false");

	/**
	 * the main class to be start a java process.
	 */
	private final String mainClass;

	/**
	 * the classpath being used to start the process.
	 */
	private String classpath = ".";

	/**
	 * the list of program arguments
	 */
	private List<String> arguments = new ArrayList<String>();

	/**
	 * the location from where java process should be started so that relative
	 * paths, if any, are maintained.
	 */
	private String location = "";

	/**
	 * relative path to the log directory
	 */
	private String relativeLogDir = "./";

	/**
	 * the list of jvm arguments
	 */
	private List<String> jvmArgs = new ArrayList<String>();

	/**
	 * the log snippet that will mark the java process started successfully
	 */
	private String logSnippet = null;

	/**
	 * for custom java home settings.
	 */
	private String javaHome = System.getProperty("java.home");

	/**
	 * log filename that contains the console output.
	 */
	private String consoleLog = "start.log";

	public ProcessConfig(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getClasspath() {
		return classpath;
	}

	public ProcessConfig setClasspath(String classpath) {
		this.classpath = classpath;
		return this;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public ProcessConfig setArguments(List<String> arguments) {
		this.arguments = arguments;
		return this;
	}

	public String getLocation() {
		return location;
	}

	public ProcessConfig setLocation(String location) {
		this.location = location;
		return this;
	}

	public String getRelativeLogDir() {
		return relativeLogDir;
	}

	public ProcessConfig setRelativeLogDir(String relativeLogDir) {
		this.relativeLogDir = relativeLogDir;
		return this;
	}

	public List<String> getJvmArgs() {
		return jvmArgs;
	}

	public ProcessConfig setJvmArgs(List<String> jvmArgs) {
		this.jvmArgs = jvmArgs;
		return this;
	}

	public String getLogSnippet() {
		return logSnippet;
	}

	public ProcessConfig setLogSnippet(String logSnippet) {
		this.logSnippet = logSnippet;
		return this;
	}

	public String getMainClass() {
		return mainClass;
	}

	public List<String> getDefaultArgs() {
		return defaultArgs;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public ProcessConfig setJavaHome(String javaHome) {
		this.javaHome = javaHome;
		return this;
	}

	public String getConsoleLog() {
		return consoleLog;
	}

	public void setConsoleLog(String consoleLog) {
		this.consoleLog = consoleLog;
	}

}
