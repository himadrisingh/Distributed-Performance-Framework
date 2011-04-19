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

public 	class ProcessThread implements Runnable{

	private static final Logger log = Logger.getLogger(ProcessThread.class);

	private final ProcessConfig config;
	private Process p;
	private final ProcessState state = new ProcessState();

	public ProcessThread(final ProcessConfig config) {
		this.config = config;
	}

	private List<String> getCmdList() {
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(System.getProperty("java.home") + "/bin/java");
		cmdList.addAll(config.getJvmArgs());
		cmdList.addAll(config.getDefaultArgs());
		cmdList.add("-Xloggc:" + config.getRelativeLogDir() + "/verboseGC.log");
		cmdList.add("-cp");
		cmdList.add(quoteIfNeeded(config.getClasspath()));
		cmdList.add(config.getMainClass());
		cmdList.addAll(config.getArguments());

		log.info("Process command: " + cmdList);
		return cmdList;

	}

	public void merge(final InputStream in, final OutputStream out) {
		StreamCopier sc = new StreamCopier(in, out, state);
		sc.setLogSnippet(config.getLogSnippet());
		sc.start();
	}

	public boolean isStarted(){
		return state.isStarted();
	}

	private static String quoteIfNeeded(final String path) {
		if (path.indexOf(" ") > 0) {
			return "\"" + path + "\"";
		}
		return path;
	}

	public void run() {
		List<String> cmdList = getCmdList();
		try {
			File logs = new File(config.getLocation() + "/" + config.getRelativeLogDir());
			logs.mkdirs();
			p = Runtime.getRuntime().exec(cmdList.toArray(new String[0]), null,
					new File(config.getLocation()));
			merge(p.getInputStream(), new FileOutputStream(logs + "/start.log"));
			merge(p.getErrorStream(), new FileOutputStream(logs + "/start.log"));
			int returnCode = p.waitFor();
			IOUtils.closeQuietly(p.getInputStream());
			IOUtils.closeQuietly(p.getOutputStream());
			IOUtils.closeQuietly(p.getErrorStream());
			p.destroy();
			log.info("Exit code: " + returnCode);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	class ProcessState {
		boolean started = false;

		public boolean isStarted() {
			return started;
		}

		public void setStarted(final boolean started) {
			this.started = started;
		}
	}

}