/**
 * 
 */
package org.tc.perf.work.items;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;

/**
 * Work item to setup the L1.
 * 
 * Downloads all client code to the L1 machine.
 */
public class SetupL1 extends AbstractL1Work {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(SetupL1.class);

	public SetupL1(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public ProcessState work() {
		FileLoader loader = new FileLoader(getDataCache());
		String targetPath = configuration.getLocation() + CLIENT_SETUP;

		ProcessState state = new ProcessState();
		try {
			loader.downloadAll(fileList, targetPath);
		} catch (IOException e) {
			log.error("Failed to download test files!", e);
			state.markFailed();
			state.setFailureReason(e.getMessage());
			return state;
		}

		// TODO: check this
		downloadLicense(loader, targetPath);

		state.markFinished();
		return state;
	}
}
