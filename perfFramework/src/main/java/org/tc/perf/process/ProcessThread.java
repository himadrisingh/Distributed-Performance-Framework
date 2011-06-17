package org.tc.perf.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.tc.perf.util.StreamCopier;

/**
 * 
 * This class start the process in a separate thread.
 * 
 * @author Himadri Singh
 */
public class ProcessThread implements Runnable {

	private static final Logger Log = Logger.getLogger(ProcessThread.class);

	/**
	 * <code>registry</code> keeps the list of the ProcessThread started on this
	 * agent. When aborting test, all the running processes can be killed.
	 */
	private static List<ProcessThread> registry = new ArrayList<ProcessThread>();

	private final ProcessConfig config;
	private Process p;
	private final ProcessState state;

	public ProcessThread(final ProcessConfig config, ProcessState state) {
		this.config = config;
		this.state = state;
	}

	private List<String> getCmdList() {
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(config.getJavaHome() + "/bin/java");
		cmdList.addAll(config.getJvmArgs());
		cmdList.addAll(config.getDefaultArgs());
		cmdList.add("-Xloggc:" + config.getRelativeLogDir() + "/verboseGC.log");
		cmdList.add("-cp");
		cmdList.add(quoteIfNeeded(config.getClasspath()));
		cmdList.add(config.getMainClass());
		cmdList.addAll(config.getArguments());
		return cmdList;
	}

	/**
	 * Merges <code>InputStream</code> to <code>OutputStream</code>, thus
	 * recording the process output to desired stream, here, to a file.
	 * 
	 * @param in
	 *            InputStream to be copied
	 * @param out
	 *            OutputStream to which data needs to be copied to.
	 */
	public void merge(final InputStream in, final OutputStream out) {
		StreamCopier sc = new StreamCopier(in, out, state);
		sc.setLogSnippet(config.getLogSnippet());
		sc.start();
	}

	/**
	 * returns the ProcessState of the process. Needed to check whether process
	 * started as expected or not.
	 * 
	 * @return ProcessState of the process
	 */

	public ProcessState getState() {
		return state;
	}

	/**
	 * Destroy the process running in the <code>ProcessThread</code>. It marks
	 * the process is being destroyed intentionally which maintains the state of
	 * the process i.e. process finished not failed.
	 */

	public void destroy() {
		if (p == null)
			return;
		Log.info("Destroying the process: " + getCmdList());
		p.destroy();
	}

	private static String quoteIfNeeded(final String path) {
		if (path.indexOf(" ") > 0) {
			return "\"" + path + "\"";
		}
		return path;
	}

	public void run() {
		registry.add(this);
		List<String> cmdList = getCmdList();
		Log.info("Process command: " + cmdList);
		try {
			File logs = new File(config.getLocation() + "/"
					+ config.getRelativeLogDir());
			logs.mkdirs();
			p = Runtime.getRuntime().exec(cmdList.toArray(new String[0]), null,
					new File(config.getLocation()));
			File logFile = new File(logs + "/" + config.getConsoleLog());
			if (logFile.exists()) {
				String name = String.format("%s/start-%d.log", logs, System
						.currentTimeMillis());
				Log.info("Renaming file start.log to " + name);
				logFile.renameTo(new File(name));
			}
			merge(p.getInputStream(), new FileOutputStream(logFile));
			merge(p.getErrorStream(), new FileOutputStream(logFile));
			state.markInitialized();
			Log.info("Log check: " + config.getLogSnippet());
			int returnCode = p.waitFor();
			IOUtils.closeQuietly(p.getInputStream());
			IOUtils.closeQuietly(p.getOutputStream());
			IOUtils.closeQuietly(p.getErrorStream());
			p.destroy();
			Log.info("Exit code: " + returnCode);
			if (returnCode == 0)
				state.markFinished();
			else {
				state.markFailed();
				state.setFailureReason("Non-Zero exit code: " + returnCode);
			}
		} catch (IOException e1) {
			state.markFailed();
			state.setFailureReason(e1.getMessage());
		} catch (InterruptedException e) {
			state.markFailed();
			state.setFailureReason(e.getMessage());
		}
		registry.remove(this);
	}

	public ProcessConfig getProcessConfig() {
		return config;
	}

	public static void killAllProcesses() {
		Log.info("Killing process on this box: " + registry.size());
		for (ProcessThread p : registry)
			p.destroy();
	}

	public static int registeredProcessCount() {
		return registry.size();
	}

}