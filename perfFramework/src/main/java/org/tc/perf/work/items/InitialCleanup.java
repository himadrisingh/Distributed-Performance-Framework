package org.tc.perf.work.items;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.CommonUtils;
import org.tc.perf.util.Configuration;

/**
 * Cleans up the local directory to remove any previous test artifacts or logs.
 * 
 * @author Himadri Singh
 */
public class InitialCleanup extends AbstractWork {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(InitialCleanup.class);

	public InitialCleanup(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public ProcessState work() {
		ProcessState state = new ProcessState();
		try {
			log.info("Initial Cleanup at " + configuration.getLocation());
			CommonUtils.deleteDir(configuration.getLocation());
		} catch (IOException e) {
			e.printStackTrace();
			state.markFailed();
			state.setFailureReason(e.getMessage());
			return state;
		}
		state.markFinished();
		return state;
	}
}
