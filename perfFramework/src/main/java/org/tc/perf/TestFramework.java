package org.tc.perf;

import static org.tc.perf.util.SharedConstants.RUNNING_TESTS;
import static org.tc.perf.util.SharedConstants.TC_CONFIG_URL;
import static org.tc.perf.util.SharedConstants.WORK_QUEUE;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.CacheGenerator;
import org.tc.perf.util.CommonUtils;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.SharedConstants;
import org.tc.perf.work.items.Work;
import org.terracotta.api.TerracottaClient;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.cluster.ClusterNode;

/**
 * 
 * This is the abstract class which creates all the distributed cache required
 * by the framework. It also maintains the work queue and access to the test
 * data.
 * 
 * @author Himadri Singh
 */
public abstract class TestFramework {

	private static final Logger log = Logger.getLogger(TestFramework.class);

	/**
	 * Not using toolkit BlockingQueue as we need to share our custom class Work
	 * and toolkit allows only literals. We can probably write our own methods
	 * to return byte[].
	 */

	private final Cache workQueue, running;

	public TestFramework() {
		this.workQueue = CacheGenerator.getCache(WORK_QUEUE);
		this.running = CacheGenerator.getCache(RUNNING_TESTS);
	}

	/**
	 * Creates a new cache specifically for test id, with name as testUniqueId
	 * 
	 * @param testUniqueId
	 *            Unique Id for test started
	 */

	protected Cache getDataCache(String testUniqueId) {
		return CacheGenerator.getCache(testUniqueId);
	}

	/**
	 * Put to workQueue, create a copyOnWriteArrayList
	 * 
	 * @param host
	 *            hostname
	 * @param workList
	 *            list of work to be done by this host
	 */

	private void putToWorkQueue(final String host, final List<Work> workList) {
		Element elem = new Element(host, workList);
		workQueue.put(elem);
	}

	/**
	 * Adds work items to the common work cache for a host
	 * 
	 * @param host
	 *            The hostname for the machine that will do the work
	 * @param work
	 *            The work to be executed
	 */

	protected void addToWorkQueue(final String host, final Work work) {
		List<Work> workList = new ArrayList<Work>(getWork(host));

		log.info(String.format("Adding work %s to %s.", work.getClass()
				.getName(), host));
		workList.add(work);

		putToWorkQueue(host, workList);
	}

	/**
	 * Updates the Work for this host in the work queue with the new state
	 * 
	 * @param work
	 *            The updated Work object
	 */

	protected void updateWork(final String host, final List<Work> work) {
		putToWorkQueue(host, work);
	}

	/**
	 * Clear the Work for this host in the work queue for error state
	 */

	protected void clearWork(String host) {
		workQueue.remove(host);
	}

	/**
	 * Add the new test record to running test cache. It adds the machines being
	 * used by the test. Required to make sure that the new test doesnt overlaps
	 * with the previous one.
	 */

	protected void addToRunningTests(Configuration config) {
		log.info("Starting test with ID : " + config.getTestUniqueId());
		getRunningTestCache().put(
				new Element(config.getTestUniqueId(), config.getAllmachines()));
	}

	/**
	 * Check to make sure that the new test machines doesnt overlaps with the
	 * previous one.
	 * 
	 * @throws IllegalStateException
	 *             if being used by any other test
	 */

	@SuppressWarnings("unchecked")
	protected void checkForUsedAgents(Configuration config) {
		List<String> superList = config.getAllmachines();

		List<String> tests = getRunningTestCache().getKeys();
		for (String t : tests) {
			List<String> machinesUsed = (List<String>) getRunningTestCache()
			.get(t).getValue();
			machinesUsed.retainAll(superList);
			if (machinesUsed.size() > 0) {
				String used = "";
				for (String m : machinesUsed)
					used += m + " , ";
				throw new IllegalStateException(used
						+ " is/are being used by test id: " + t);
			}
		}
	}

	/**
	 * Checks for connected Agents
	 * 
	 * @throws IllegalStateException
	 *             if required agents are not connected
	 */

	protected void checkConnectedAgents(Configuration config) {
		Collection<String> agents = getConnectedAgents();
		if (agents.isEmpty()) {
			throw new IllegalStateException(
					"No Agents are found connected to f/w terracotta server: "
					+ SharedConstants.TC_CONFIG_URL
					+ ". Are you sure Agents are running?");
		}

		List<String> superList = new ArrayList<String>(config.getAllmachines());
		if (superList.removeAll(agents) && superList.size() > 0) {
			String missing = "";
			for (String m : superList)
				missing += m + " , ";

			throw new IllegalStateException(
					"Not all agents required by this test are connected to fw server: "
					+ SharedConstants.TC_CONFIG_URL
					+ ". Missing Agents List: " + missing);
		}
	}

	/**
	 * Returns the list of connected agents to <code>TC_CONFIG_URL</code>
	 * 
	 * @return {@link List} list of connected agents
	 */

	private List<String> getConnectedAgents() {
		ClusterInfo info = (new TerracottaClient(TC_CONFIG_URL)).getToolkit()
		.getClusterInfo();
		Collection<ClusterNode> listOfAgents = info.getClusterTopology()
		.getNodes();

		// Remove Master Node from the agent lists
		listOfAgents.remove(info.getCurrentNode());

		List<String> agentNames = new ArrayList<String>();
		for (ClusterNode agent : listOfAgents) {
			try {
				agentNames
				.add((agent.getAddress().getHostName().toLowerCase()));
				log.debug("Agents found: "
						+ agent.getAddress().getHostName().toLowerCase());
			} catch (UnknownHostException e) {
				log.error("Unknown Agent found " + agent, e);
			}
		}
		return agentNames;
	}

	/**
	 * Gets work items from the common work cache for a host CopyOnRead list.
	 * 
	 * @param host
	 *            The hostname for the machine is doing the work
	 * 
	 * @return the Work instance for this host. If no work was found, null will
	 *         be returned.
	 */

	protected List<Work> getWork(String host) {
		Element e = workQueue.get(host);
		if (e != null) {
			Serializable val = e.getValue();
			if (val instanceof List<?>)
				return new ArrayList<Work>((List<Work>) val);
			else
				throw new IllegalStateException(
						"List of work expected but got " + val.getClass());
		} else
			return Collections.emptyList();
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
	protected void waitForWorkCompletion(final List<String> hosts,
			final int timeoutMins) throws Exception {
		List<String> pendingHosts = new ArrayList<String>(hosts);
		Calendar timeout = Calendar.getInstance();
		timeout.add(Calendar.MINUTE, timeoutMins);
		while (!pendingHosts.isEmpty()) {
			for (String host : hosts) {
				List<Work> workList = getWork(host);

				Iterator<Work> iter = workList.iterator();
				while (iter.hasNext()) {
					Work work = iter.next();
					ProcessState state = work.getState();
					if (state.isStarted() || state.isFinished())
						iter.remove();

					if (state.isTimeout()) {
						iter.remove();
						throw new TimeoutException("Test job failed since "
								+ host + " didnt completed the task due to "
								+ state.getFailureReason());
					}
					if (state.isFailed()) {
						iter.remove();
						throw new Exception("Job execution Failed. Reason: "
								+ state.getFailureReason());
					}
				}
				if (workList.isEmpty()) {
					clearWork(host);
					pendingHosts.remove(host);
				}
			}
		}
		CommonUtils.sleep(1000);
		if (Calendar.getInstance().after(timeout)) {
			log.error("Timed out waiting for jobs to finish on: " + hosts);
			throw new TimeoutException("Exceeded timeout of " + timeoutMins
					+ " minutes");
		}
	}

	/**
	 * Returns the cache containing the running tests unique id and machines
	 * used by them.
	 * 
	 * @return cache containing running tests
	 */

	public Cache getRunningTestCache() {
		return running;
	}

}
