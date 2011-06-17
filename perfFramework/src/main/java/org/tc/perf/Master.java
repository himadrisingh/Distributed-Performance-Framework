package org.tc.perf;

import static org.tc.perf.util.SharedConstants.FILELIST;
import static org.tc.perf.util.SharedConstants.KIT_NAME;
import static org.tc.perf.util.SharedConstants.WORK_TIME_OUT_MINUTES;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;
import org.tc.perf.work.items.MasterWork;
import org.tc.perf.work.items.Work;

/**
 * 
 * Master class thats responsible for loading all the test artifacts to the
 * distributed cache and initiating the test process. It adds the
 * <code>MasterControl</code> work to one of the agents.
 * 
 * @author Himadri Singh
 */

public class Master extends TestFramework {

	private static Logger log = Logger.getLogger(Master.class);
	private final Configuration config;
	private Cache data;

	public Master(final Configuration config) {
		this.config = config;
	}

	/**
	 * It uploads the test jars, license and the terracotta kit specified in the
	 * configuration.
	 */
	private void uploadTestJars() {
		log.info("Uploading jars/files ...");
		FileLoader loader = new FileLoader(data);
		List<String> regexList = config.getClasspathRegex();
		List<Pattern> patterns = new ArrayList<Pattern>();
		for (String regex : regexList) {
			patterns.add(Pattern.compile(regex));
		}
		List<String> files = null;
		try {
			File kit = new File(config.getKitLocation());
			log.info("Uploading terracotta kit: " + kit.getAbsolutePath());
			loader.uploadSingleFile(kit);
			data.put(new Element(KIT_NAME, kit.getName()));

			log.info("Uploading test files...");
			File license = new File(config.getLicenseFileLocation());
			if (license.exists())
				loader.uploadSingleFile(license);
			else
				log
				.warn("***** License file NOT found. EE version might not work. *****");
			files = loader.uploadDirectories(config.getTestDirectories(),
					patterns);
		} catch (IOException e) {
			throw new RuntimeException(
					"Failed to upload test artifacts to cache.", e);
		}
		data.put(new Element(FILELIST, files));
		log.info("Finished uploading all the required files.");
	}

	/**
	 * This method performs the basic checks and starts the test process. It
	 * adds the <code>MasterControl</code> work to first l2 machine. That
	 * machine will act as the controller to the test. Master process can exit
	 * without worrying about the test.
	 */

	public void run() {
		checkConnectedAgents(config);
		checkForUsedAgents(config);
		data = getDataCache(config.getTestUniqueId());

		uploadTestJars();
		Work work = new MasterWork(config);
		String host = config.getL2machines().get(0);

		addToWorkQueue(host, work);

		List<String> hosts = new ArrayList<String>();
		hosts.add(host);
		try {
			waitForWorkCompletion(hosts, WORK_TIME_OUT_MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
		}
		addToRunningTests(config);
		log.info(String.format(
				"Test has been initiated. %s would be controlling the test. ",
				host));
	}
}
