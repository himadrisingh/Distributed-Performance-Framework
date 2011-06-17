package org.tc.perf.work.items;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.CacheGenerator;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;

/**
 * Abstract base class for work
 * 
 * Base class for "Work" implementations. Maintains the configuration and
 * completion state of the work at hand.
 * 
 * @see Work for more details.
 */

public abstract class AbstractWork implements Work {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(AbstractWork.class);

	protected final Configuration configuration;
	private ProcessState state = new ProcessState();

	public AbstractWork(final Configuration configuration) {
		this.configuration = configuration;
	}

	protected Cache getDataCache(){
		return CacheGenerator.getCache(configuration.getTestUniqueId());
	}

	public void doWork(){
		state.markInitialized();
		state = work();
	}

	/**
	 * Abstract method for actual work implementation.
	 */
	protected abstract ProcessState work();

	/**
	 * Set the internal state of this work as complete.
	 */
	public ProcessState getState() {
		return this.state;
	}

	/**
	 * Download a license file from the cache to the local filesystem
	 * 
	 * @param loader
	 * @param licensePath
	 */

	protected void downloadLicense(final FileLoader loader, final String licensePath) {
		String licenseFileName = (new File(
				configuration.getLicenseFileLocation())).getName();
		try {
			loader.download(licenseFileName, licensePath);
		} catch (FileNotFoundException e) {
			log.warn("No license file was found in the cache. You may see failures if you use enterprise features.");
		} catch (IOException e) {
			log.warn(
					"Error downloading the license file from the cache. You may see errors if you use enterprise features.",
					e);
		}
	}

}
