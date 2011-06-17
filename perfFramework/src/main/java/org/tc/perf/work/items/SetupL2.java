/**
 * 
 */
package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.TC_INSTALL_DIR;

import java.io.IOException;

import net.sf.ehcache.Element;

import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;
import org.tc.perf.util.TcConfigBuilder;

/**
 * 
 * Work item to set up the L2.
 * 
 * Sets up the L2 by downloading and extracting the kit and building the
 * tc-config.xml
 * 
 * @author Himadri Singh
 */

public class SetupL2 extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public SetupL2(final Configuration configuration) {
		super(configuration);
	}

	private void setTcInstallDir(String installDir) {
		getDataCache().put(new Element(TC_INSTALL_DIR, installDir));
	}

	@Override
	protected ProcessState work() {
		ProcessState state = new ProcessState();
		FileLoader loader = new FileLoader(getDataCache());
		String kitPath;
		try {
			kitPath = loader.downloadExtractKit(configuration.getLocation());
			setTcInstallDir(kitPath);
			downloadLicense(loader, kitPath);

			TcConfigBuilder builder = new TcConfigBuilder(configuration);
			builder.createConfig(kitPath);
		} catch (IOException e) {
			state.markFailed();
			state.setFailureReason(e.getMessage());
			return state;
		}
		state.markFinished();
		return state;
	}
}
