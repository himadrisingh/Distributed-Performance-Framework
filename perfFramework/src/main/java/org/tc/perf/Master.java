package org.tc.perf;

import static org.tc.perf.util.SharedConstants.CONFIG;
import static org.tc.perf.util.SharedConstants.FILELIST;
import static org.tc.perf.util.SharedConstants.HOSTNAME;
import static org.tc.perf.util.SharedConstants.MAIN_CLASS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.ClusterWatcher;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;
import org.tc.perf.work.items.CleanupL1;
import org.tc.perf.work.items.CleanupL2;
import org.tc.perf.work.items.InitialCleanup;
import org.tc.perf.work.items.SetupL1;
import org.tc.perf.work.items.SetupL2;
import org.tc.perf.work.items.StartL1;
import org.tc.perf.work.items.StartL2;
import org.tc.perf.work.items.StopL2;
import org.tc.perf.work.items.Work;

public class Master extends PerfFramework {

	private static final int WORK_TIME_OUT_MINUTES = 120;
	private static Logger log = Logger.getLogger(Master.class);
	private final Configuration config;

	public Master(final Configuration config) {
		this.config = config;
	}

	public void run() {
		log.info("Checking for unfinished previous test runs.");
		if (notification.getSize() > 0){
			throw new RuntimeException("Some test is running already.");
		}
		reset();
		log.debug("Cache size: " + test.getSize());
		log.info("Starting test...");
		test.put(new Element(MAIN_CLASS, config.getMainClass()));
		test.put(new Element(CONFIG, config));
		uploadTestJars();
		try {
			executeTestProcess();
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"Timed out waiting for test processes to finish", e);
		}
	}

	private void uploadTestJars(){
		log.info("Uploading jars/files ...");
		FileLoader loader = new FileLoader(test);
		List<String> regexList = config.getClasspathRegex();
		List<Pattern> patterns = new ArrayList<Pattern>();
		for (String regex: regexList){
			patterns.add(Pattern.compile(regex));
		}
		try {
			loader.uploadKit(config.getKitLocation());
		} catch (IOException e1) {
			reset();
			throw new RuntimeException("Failed to upload the kit." , e1);
		}
		log.info("Uploading test files...");
		List<String> files = null;
		try {
			File license = new File(config.getLicenseFileLocation());
			if (license.exists())
				loader.uploadSingleFile(license);
			else
				log.warn("***** License file NOT found. EE version might not work. *****");
			files = loader.uploadDirectories(config.getTestDirectories(),
					patterns);
		} catch (IOException e) {
			reset();
			throw new RuntimeException(
					"Failed to upload test artifacts to cache.", e);
		}
		test.put(new Element(FILELIST, files));
		log.info("Finished uploading all the required files.");
	}

	/**
	 * Adds work items to the common work cache for a host
	 * 
	 * @param host
	 *            The hostname for the machine that will do the work
	 * @param work
	 *            The work to be executed
	 */
	private void addWork(final String host, final Work work) {
		log.info(String.format("Adding work %s to %s.", work.getClass().getName(), host));
		Element elem = new Element(host, work);
		notification.put(elem);
	}

	/**
	 * Gets work items from the common work cache for a host
	 * 
	 * @param host
	 *            The hostname for the machine is doing the work
	 * 
	 * @return the Work instance for this host. If no work was found, null will
	 *         be returned.
	 */
	private Work getWork(final String host) {
		Element elem = notification.get(host);

		return (elem == null) ? null : (Work) elem.getValue();
	}

	private void executeTestProcess() throws TimeoutException {

		log.info("Executing test process...");

		List<String> l2machines = config.getL2machines();
		List<String> l1machines = config.getL1machines();
		boolean isLoadNeeded = (config.getLoadmachines().size() > 0) ? Boolean.TRUE : Boolean.FALSE;

		for (String l2 : l2machines)
			addWork(l2, new InitialCleanup(config));

		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		for (String l1 : l1machines)
			addWork(l1, new InitialCleanup(config));

		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		// Setup L2
		log.info("Setup L2 on all l2_machines: " + l2machines);
		for (String l2 : l2machines)
			addWork(l2, new SetupL2(config));

		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		// Setup L1
		log.info("Setup L1 on all l1_machines: " + l1machines);
		for (String l1: l1machines)
			addWork(l1, new SetupL1(config));

		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		// Start L2
		log.info("Start L2 on all l2_machines: " + l2machines);
		for (String l2: l2machines)
			addWork(l2, new StartL2(config));
		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		startClusterWatcher();

		// Start l1
		log.info("Start L1 on all l1_machines: " + l1machines);
		for (String l1: l1machines)
			addWork(l1, new StartL1(config));
		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		if (isLoadNeeded){
			// TODO: Add load process
			// TODO: Stop L1 Process
		} else
			log.info("Clients should have finished.");

		// Stop L2
		log.info("Stopping L2 on all l2_machines: " + l2machines);
		for (String l2: l2machines)
			addWork(l2, new StopL2(config));
		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		log.info("Cleanup L1 on all l1_machines: " + l1machines);
		for (String l1: l1machines)
			addWork(l1, new CleanupL1(config));
		waitForWorkCompletion(l1machines, WORK_TIME_OUT_MINUTES);

		log.info("Cleanup L2 on all l2_machines: " + l2machines);
		for (String l2: l2machines)
			addWork(l2, new CleanupL2(config));
		waitForWorkCompletion(l2machines, WORK_TIME_OUT_MINUTES);

		ArrayList<String> logs = new ArrayList<String>();
		for (String l2: l2machines)
			logs.add("server-" + l2 + "-logs.tar.gz");

		for (String l1: l1machines)
			logs.add("client-" + l1 + "-logs.tar.gz");

		FileLoader loader = new FileLoader(test);
		try {
			loader.downloadAll(logs, "test-run");
		} catch (IOException e) {
			log.error("Failed to download logs for the test.", e);
		}
		reset();
	}

	/*    private void waitForAll(){
        while (true) {
        	List<String> hosts = notification.getKeys();
        	boolean flag = true;
        	for (String host: hosts){
        		Work work = (Work) notification.get(host).getValue();
        		if (!work.isWorkDone()){
        			flag = false;
        		}
        	}
        	if (flag){
        		log.info("Work completed.");
        		notification.removeAll();
        		return;
        	}
        	else
        		sleep(1000);
        }
    }
	 */

	private void startClusterWatcher() {
		Thread clusterWatcher = new Thread(new Runnable() {

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
				props.setProperty("clientcount", String.valueOf(config.getL1machines().size()));
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

	/**
	 * Wait for work to finish on all hosts.
	 * 
	 * @param hosts
	 *            A list of hosts to wait for.
	 * @param timeoutMins
	 *            A timeout in minutes for the jobs to complete.
	 * 
	 * @throws TimeoutException
	 *             If the job doesn't complete within the specified timeout
	 */
	private void waitForWorkCompletion(final List<String> hosts,
			final int timeoutMins) throws TimeoutException {
		List<String> pendingHosts = new ArrayList<String>(hosts);
		Calendar timeout = Calendar.getInstance();
		timeout.add(Calendar.MINUTE, timeoutMins);
		while (!pendingHosts.isEmpty()) {
			for (String host : hosts) {
				Work w = getWork(host);
				if (w == null) {
					pendingHosts.remove(host);
				} else if (w.isWorkDone()) {
					notification.remove(host);
				}
			}
			sleep(1000);
			if (Calendar.getInstance().after(timeout)) {
				log.error("Timed out waiting for jobs to finish on: " + hosts);
				throw new TimeoutException("Exceeded timeout of " + timeoutMins
						+ " minutes");
			}
		}
	}

	public static void main(final String[] args) {
		if (args.length == 0){
			log.fatal("Usage: " + Master.class + " <configuration-file-name>");
			System.exit(1);
		}
		log.info(String.format("Starting performance framework master on %s ...", HOSTNAME));
		Configuration config = new Configuration(args[0]);
		Master master = new Master(config);
		master.run();
	}

}
