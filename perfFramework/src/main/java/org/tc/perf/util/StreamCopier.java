package org.tc.perf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;

/**
 * 
 * Utility class that can copy data from an InputStream into an OutputStream.
 * This used to copy output from a running process into a file or console. This
 * also checks for a log snippet on runtime, if found, marks the process state
 * as started.
 * 
 * @author Himadri Singh
 */
public class StreamCopier extends Thread {

	private static final Logger log = Logger.getLogger(StreamCopier.class);
	private final OutputStream out;
	private final BufferedReader reader;
	private String logSnippet;
	private final ProcessState state;
	private boolean isStarted = false;

	public StreamCopier(InputStream stream, OutputStream out, ProcessState state) {
		if ((stream == null) || (out == null)) {
			throw new AssertionError("null streams not allowed");
		}

		reader = new BufferedReader(new InputStreamReader(stream));
		this.out = out;
		this.state = state;

		setName("Stream Copier");
		setDaemon(true);
	}

	public void setLogSnippet(String logSnippet) {
		this.logSnippet = logSnippet;
	}

	/**
	 * Copies data from input stream to output stream. Also matches for the log
	 * snippet and marks the process state started if found.
	 */
	@Override
	public void run() {
		final String newLine = System.getProperty("line.separator", "\n");
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				line += newLine;
				if (!isStarted && logSnippet != null) {
					if (line.toLowerCase().indexOf(logSnippet.toLowerCase()) >= 0
							|| logSnippet.trim().length() == 0) {
						isStarted = true;
						log.info("Process marked STARTED.");
						state.markStarted();
					}
				}
				out.write(line.getBytes());
				out.flush();
			}
		} catch (IOException ioe) {
			log
			.fatal("Probably process exited abnormally: "
					+ ioe.getMessage());
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
}
