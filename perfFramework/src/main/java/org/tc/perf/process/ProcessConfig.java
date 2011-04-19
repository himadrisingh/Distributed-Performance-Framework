package org.tc.perf.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessConfig {

	private final List<String> defaultArgs = Arrays.asList(
			"-XX:+HeapDumpOnOutOfMemoryError", "-XX:+PrintGCDateStamps",
			"-XX:+PrintTenuringDistribution", "-verbose:gc",
			"-XX:+PrintGCApplicationStoppedTime", "-XX:+PrintHeapAtGC",
			"-Dcom.sun.management.jmxremote", "-showversion",
			"-Dcom.sun.management.jmxremote.ssl=false","-Dcom.sun.management.jmxremote.authenticate=false");
	//,"-Dcom.sun.management.jmxremote.port=9240"
	private final String  mainClass;
	private String classpath = ".";
	private List<String> arguments = new ArrayList<String>();
	private String location = "";
	private String relativeLogDir = "./";
	private List<String> jvmArgs = new ArrayList<String>();
	private String logSnippet = "";

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

	public ProcessConfig  waitForLogSnippet(String logSnippet) {
		this.logSnippet = logSnippet;
		return this;
	}

	public String getMainClass() {
		return mainClass;
	}

	public List<String> getDefaultArgs() {
		return defaultArgs;
	}
}
