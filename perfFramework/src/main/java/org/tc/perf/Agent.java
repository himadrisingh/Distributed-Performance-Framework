package org.tc.perf;

import static org.tc.perf.util.SharedConstants.HOSTNAME;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.CommonUtils;
import org.tc.perf.work.items.Work;

/**
 * Agent is the process started on machines used in testing. This will take care
 * of <code>Work</code> alloted to this machine. It can spawn different
 * processes and is responsible for the log collection, etc.
 * 
 * @author Himadri Singh
 * 
 */

public class Agent extends TestFramework {

	private static final Logger log = Logger.getLogger(Agent.class);
	private static final int pollPeriod = 500;

	/**
	 * This method keeps in polling <code>workQueue</code> for the Work alloted
	 * to this agent. The list of Work is extracted from the cache and each Work
	 * is executed in a separate thread.
	 * 
	 * It also checks if Work is completed/aborted it should be removed in 500 *
	 * 200 ms.
	 * 
	 */

	public void poll() {
		log.info(HOSTNAME + " Agent ready for work from master.");
		int i = 0;
		while (true) {
			List<Work> workList = getWork(HOSTNAME);
			for (Work work : workList) {
				ProcessState state = work.getState();
				if (state.isNotStarted()) {
					Executor exec = Executors.newSingleThreadExecutor();
					exec.execute(new ProcessWork(work));
				} else {
					if ((state.isFailed() || state.isFinished()
							|| state.isTimeout() || state.isStarted())
							&& i++ > 200) {
						log
						.error("Work is not being removed by Master. "
								+ "Seems there a failure at MasterControl? "
								+ "Clearing the Work State: "
								+ work.getState());
						clearWork(HOSTNAME);
						i = 0;
					}
				}
			}
			updateWork(HOSTNAME, workList);
			CommonUtils.sleep(pollPeriod);
		}
	}

	/**
	 * @author Himadri Singh
	 * 
	 *         Internal class to execute each Work in separate thread. This is
	 *         done because each execution is blocking call so to execute
	 *         multiple Work in same box needs to be executed in different
	 *         threads.
	 */
	private static class ProcessWork implements Runnable {

		final Work work;

		public ProcessWork(Work work) {
			this.work = work;
			log.info("Starting thread for processing work: " + work.getClass());
		}

		public void run() {
			work.doWork();
		}
	}
}
