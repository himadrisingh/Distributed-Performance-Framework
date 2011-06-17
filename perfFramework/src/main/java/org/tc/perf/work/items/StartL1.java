/**
 * 
 */
package org.tc.perf.work.items;

import java.util.concurrent.atomic.AtomicInteger;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessExecutor;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;

/**
 * @author gautam
 *
 */
public class StartL1 extends AbstractL1Work {

	private static final long serialVersionUID = 1L;
	private static AtomicInteger clientsStarted = new AtomicInteger();

	public StartL1(final Configuration configuration) {
		super(configuration);
	}

	@Override
	protected ProcessState work() {
		ProcessConfig config = getProcessConfig();
		config.setConsoleLog("client-start-" + clientsStarted.get() + ".log");
		clientsStarted.incrementAndGet();
		long wait = -1;
		if (configuration.getLoadmachines().size() > 0){
			config.setLogSnippet(configuration.getClientLogCheck());
			log.info("loadmachines are configured. Timeout set to 120 secs to check client started successfully.");
			wait = 120;
		}
		ProcessExecutor exec = new ProcessExecutor(config);
		return exec.executeAndWaitFor(wait);
	}

}
