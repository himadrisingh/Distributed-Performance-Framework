/**
 * 
 */
package org.tc.perf.work.items;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessExecutor;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;

/**
 * @author Himadri Singh
 *
 */
public class StartLoad extends AbstractLoadWork {

	private static final long serialVersionUID = 1L;

	public StartLoad(final Configuration configuration) {
		super(configuration);
	}

	@Override
	protected ProcessState work() {
		ProcessConfig config = getProcessConfig();
		ProcessExecutor exec = new ProcessExecutor(config);
		return exec.executeAndWaitFor(-1);
	}

}
