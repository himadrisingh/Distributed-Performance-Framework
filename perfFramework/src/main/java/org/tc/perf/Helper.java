package org.tc.perf;

import java.util.List;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.util.CacheGenerator;
import org.tc.perf.work.items.AbortWork;

/**
 * Helper class to list running tests and kill a running test.
 * 
 * @author Himadri Singh
 */
public class Helper extends TestFramework {

	private static final Logger log = Logger.getLogger(Helper.class);

	/**
	 * Lists all the registered tests running in the framework.
	 */

	@SuppressWarnings("unchecked")
	public void listRunningTests() {
		List<String> keys = getRunningTestCache().getKeys();
		if (keys.size() > 0) {
			log.info("List of running tests in the framework.\n");
			log.info("S.No\tTest ID\t\t\t\t\tMachines used");
			log
			.info("========================================================================");
			int i = 1;
			for (String k : keys) {
				List<String> mac = (List<String>) getRunningTestCache().get(k)
				.getValue();
				String machines = "";
				for (String m : mac)
					machines += m + " , ";
				log.info(String.format("%d.\t%s\t%s", i, k, machines));
			}
		} else
			log.info("No tests are running in the framework.");
	}

	/**
	 * Removes the test from running test lists. Clears the data cache created
	 * for the test.
	 * 
	 * @param testUniqueId
	 */

	@SuppressWarnings("unchecked")
	public void killTest(String testUniqueId) {

		Element e = getRunningTestCache().get(testUniqueId);
		if (e != null) {
			List<String> hosts = (List<String>) e.getValue();
			// Clear the work queue first.
			log.info("Clearing work queue...");
			for (String h : hosts) {
				clearWork(h);
				addToWorkQueue(h, new AbortWork(null));
			}
			try {
				waitForWorkCompletion(hosts, 120);
				CacheGenerator.removeCache(testUniqueId);
				getRunningTestCache().remove(testUniqueId);
				log.info("Test with id " + testUniqueId
						+ " has been cleaned and removed from the framework.");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else
			log.info(String.format("Test %s not found!!!", testUniqueId));

	}

}
