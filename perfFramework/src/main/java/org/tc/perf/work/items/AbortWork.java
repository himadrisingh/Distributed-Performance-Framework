package org.tc.perf.work.items;

import org.tc.perf.process.ProcessState;
import org.tc.perf.process.ProcessThread;
import org.tc.perf.util.Configuration;

/**
 * Work that kills all the process running on alloted host.
 * 
 * @author Himadri Singh
 * 
 */
public class AbortWork extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public AbortWork(Configuration configuration) {
		super(configuration);
	}

	@Override
	protected ProcessState work() {
		ProcessState state = new ProcessState();
		ProcessThread.killAllProcesses();
		state.markFinished();
		return state;
	}

}
