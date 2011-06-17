package org.tc.perf.process;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.tc.perf.util.CommonUtils;

/**
 * <code>ProcessExecutor</code> is used to run java process. This class starts a
 * thread which in turn runs a java process defined by
 * <code>ProcessConfig</code>. <code>ProcessThread</code> gives access to
 * <code>ProcessState</code> through which <code>ProcessExecutor</code> makes
 * sure that process started as expected.
 * 
 * @author Himadri Singh
 */

public class ProcessExecutor {

	private static final Logger log = Logger.getLogger(ProcessExecutor.class);
	private final Executor exec = Executors.newCachedThreadPool();
	private final ProcessThread process;
	private ProcessState state;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            The process config required to be executed.
	 */
	public ProcessExecutor(final ProcessConfig config) {
		this.state = new ProcessState();
		this.process = new ProcessThread(config, state);
	}

	/**
	 * Starts the Process Thread.
	 */

	private void executeThread() {
		exec.execute(process);
		CommonUtils.sleep(2000);
	}

	/**
	 * Gets the process config for a particular executor
	 * 
	 * @return process config
	 */
	public ProcessConfig getProcessConfig() {
		return process.getProcessConfig();
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Executes the process and removes it from the registry after completion.
	 * 
	 * @param timeout
	 * @return
	 */
	public ProcessState executeAndWaitFor(final long timeout) {
		ProcessState state = execute(timeout);
		// registry.remove(process);
		return state;
	}

	private ProcessState execute(final long timeout) {
		log.info("Starting process.");
		executeThread();
		log.info(String.format("Timeout set to %d secs.", timeout));
		long end = now() + (timeout * 1000);

		while (now() < end || timeout < 0) {
			try {
				Thread.sleep(500);
				state = process.getState();
				if (state.isStarted() || state.isFinished() || state.isFailed())
					return state;
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				state.markTimeout();
				state.setFailureReason(e.getMessage());
				return state;
			}
		}
		log.error("Process TIMED OUT...");
		state.markTimeout();
		state.setFailureReason("Process timed out after " + timeout + " secs");
		return state;
	}

}
