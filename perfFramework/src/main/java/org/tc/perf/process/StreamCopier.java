package org.tc.perf.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessThread.ProcessState;

public class StreamCopier extends Thread {

	private static final Logger log = Logger.getLogger(StreamCopier.class);
	private final OutputStream out;
	private final BufferedReader reader;
	private String logSnippet;
	private ProcessState state;

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
		log.info(logSnippet);
	}

	public void run() {
		final String newLine = System.getProperty("line.separator", "\n");
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				line += newLine;
				if (line.toLowerCase().indexOf(logSnippet.toLowerCase()) >= 0){
					state.setStarted(true);
				}
				out.write(line.getBytes());
				out.flush();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			log.fatal("Probably process exited abnormally.");
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
}
