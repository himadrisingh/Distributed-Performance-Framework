package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.WORK_TIME_OUT_MINUTES;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.ClusterWatcher;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;
import org.tc.perf.TestFramework;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;

/**
 * 
 * This is the test controller which distributes the Work to required hosts and
 * checks whether the alloted task was executed successfully or not. On any
 * error/exception, it starts the cleanup process and downloads the test logs to
 * local directory.
 * 
 * @author Himadri Singh
 */
public class MasterControl extends TestFramework implements Runnable {

	private static final Logger log = Logger.getLogger(MasterControl.class);

	private final Configuration config;
	private final List<String> l2machines, l1machines;
	private final boolean isLoadNeeded;
	private Thread clusterWatcher;

	public MasterControl(Configuration configuration) {
		config = configuration;
		l1machines = config.getL1machines();
		l2machines = config.getL2machines();
		isLoadNeeded = (config.getLoadmachines().size() > 0) ? Boolean.TRUE
				: Boolean.FALSE;
	}

	public void run() {
		try {
			executeTestProcess();
		} catch (Exception e) {
			log.error("Test Process aborted.", e);
		}
		executeCleanup();
		collectLogs();
	}

	private void executeTestProcess() throws Exception {
		log.info("Executing test process...");

		HashSet<String> noDuplicateL2 = new HashSet<String>(l2machines);
		HashSet<String> noDuplicateL1 = new HashSet<String>(l2machines);

		for (String l2 : noDuplicateL2)
			addToWorkQueue(l2, new InitialCleanup(config));

		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		for (String l1 : noDuplicateL1)
			addToWorkQueue(l1, new InitialCleanup(config));

		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		// Setup L2
		log.info("Setup L2 on all l2_machines: " + l2machines);

		for (String l2 : noDuplicateL2)
			addToWorkQueue(l2, new SetupL2(config));

		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		// Setup L1
		log.info("Setup L1 on all l1_machines: " + l1machines);
		for (String l1 : noDuplicateL1)
			addToWorkQueue(l1, new SetupL1(config));

		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		// Start L2
		log.info("Start L2 on all l2_machines: " + l2machines);
		for (String l2 : l2machines)
			addToWorkQueue(l2, new StartL2(config));
		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		startClusterWatcher();

		// Start l1
		log.info("Start L1 on all l1_machines: " + l1machines);
		for (String l1 : l1machines)
			addToWorkQueue(l1, new StartL1(config));
		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		if (isLoadNeeded) {
			// TODO: Add load process
			// TODO: Stop L1 Process
		} else
			log.info("Clients should have finished.");
	}

	private void collectLogs() {
		ArrayList<String> logs = new ArrayList<String>();
		for (String l2 : l2machines)
			logs.add("server-" + l2 + "-logs.tar.gz");

		for (String l1 : l1machines)
			logs.add("client-" + l1 + "-logs.tar.gz");

		Cache data = getDataCache(config.getTestUniqueId());
		FileLoader loader = new FileLoader(data);
		try {
			loader.downloadAll(logs, "logs");

			List<Pattern> patterns = new ArrayList<Pattern>();
			patterns.add(Pattern.compile(".*log"));
			patterns.add(Pattern.compile(".*csv"));
			patterns.add(Pattern.compile(".*gz"));

			List<File> dirs = new ArrayList<File>();
			dirs.add(new File("logs"));
			File results = new File("run-logs.tar.gz");
			loader.gzipFiles(dirs, patterns, results);
		} catch (IOException e) {
			log
			.error("Failed to download logs for the test. "
					+ e.getMessage());
		}
	}

	private void executeCleanup() {
		if (clusterWatcher != null)
			clusterWatcher.interrupt();

		try {
			// Stop L2
			log.info("Stopping L2 on all l2_machines: " + l2machines);
			for (String l2 : l2machines)
				addToWorkQueue(l2, new StopL2(config));

			waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);
		} catch (Exception e) {
			log.error("Stopping L2 failed: " + e.getMessage());
		}
		try {
			log.info("Cleanup L1 on all l1_machines: " + l1machines);
			for (String l1 : l1machines)
				addToWorkQueue(l1, new CleanupL1(config));

			waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);
		} catch (Exception e) {
			log.error("Cleanup L1 failed: " + e.getMessage());
		}
		try {
			log.info("Cleanup L2 on all l2_machines: " + l2machines);
			for (String l2 : l2machines)
				addToWorkQueue(l2, new CleanupL2(config));

			waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);
		} catch (Exception e) {
			log.error("Cleanup L2 failed: " + e.getMessage());
		}

	}

	private void startClusterWatcher() {
		clusterWatcher = new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				Properties props = new Properties();
				String tcConfig = "";
				for (String l2 : config.getL2machines())
					tcConfig += l2 + ":9510,";

				props.setProperty("tc-config.url", tcConfig);
				props.setProperty("clientcount", String.valueOf(config
						.getL1machines().size()));
				ClusterWatcherProperties.loadProperties(props);
				try {
					ClusterWatcher watcher = new ClusterWatcher();
					watcher.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		clusterWatcher.start();
	}

}
