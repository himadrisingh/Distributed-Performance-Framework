package org.tc.perf.work.items;

import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;

/**
 * This class starts the MasterControl in a separate thread and notifies the
 * Master that the test has been initiated.
 * 
 * @author Himadri Singh
 */
public class MasterWork extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public MasterWork(Configuration configuration) {
		super(configuration);
	}

	@Override
	protected ProcessState work() {
		ProcessState state = new ProcessState();

		Thread master = new Thread(new MasterControl(configuration),
		"MasterControl");
		master.start();

		state.markStarted();
		return state;
	}

}
