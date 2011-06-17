package org.tc.cluster.watcher;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.logger.AbstractLogger;
import org.tc.cluster.watcher.logger.StatsLogger;
import org.tc.cluster.watcher.logger.SystemStatsLogger;
import org.tc.cluster.watcher.notification.DGCNotificationListener;
import org.tc.cluster.watcher.notification.OperatorEventsNotificationListener;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;
import org.tc.cluster.watcher.util.Utils;


public class ClusterWatcher {
	private static final Logger LOG = Logger.getLogger(ClusterWatcher.class);
	private static final Logger FATAL = Logger.getLogger("fatalLogs");

	private int lowTxrProbeCount;
	private int clusterDownProbeCount;
	private int missingClientsProbeCount;
	private final AbstractLogger tcStats = new StatsLogger();
	private final AbstractLogger sysStats = new SystemStatsLogger();

	private ArrayList<MirrorGroup> clusterList;

	public ClusterWatcher() throws Exception {
		int count = 0;
		do {
			this.clusterList = Utils.getHostAndJMXStringFromTcConfig(ClusterWatcherProperties.TC_CONFIG);
			if (this.clusterList == null){
				LOG.error("Cluster is not up. Retrying in 5 secs...");
				Thread.sleep(5000);
				count++;
			}
		} while (clusterList == null & count < ClusterWatcherProperties.INITIAL_RETRIES);
		if (clusterList == null){
			throw new Exception("Cluster is not up.");
		}

		ServerStat active = getActiveCoordinator();
		registerEvents(active);

		LOG.info("ClusterWatcher initialized. Cluster: ");
		Iterator<MirrorGroup> iterator = clusterList.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			MirrorGroup mg = iterator.next();
			LOG.info("Mirror Group " + ++i + ": " + mg);
		}
	}

	private void registerEvents(ServerStat active){
		try {
			active.registerDsoNotificationListener(new DGCNotificationListener());
		} catch (Exception e) {
			LOG.error("Error in registering DSO Notification Listener : "
					+ e.getMessage());
		}
		try {
			active.registerEventNotificationListener(new OperatorEventsNotificationListener(active));
		} catch (Exception e) {
			LOG
			.error("Error in registering Event Notification Listener. Are u running OS kit? : "
					+ e.getMessage());
		}
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(ClusterWatcherProperties.PROBE_INTERVAL);
			} catch (InterruptedException e) {
				LOG.info("ClusterWatcher Finished.");
				return;
			}
			if (!allServerOnline()){
				FATAL.error("##### ALL SERVERS ARE NOT ONLINE. #####");
			}
			ServerStat serverStat = getActiveCoordinator();
			if (serverStat != null) {
				LOG.info(serverStat + " is ACTIVE-COORDINATOR");
				clusterDownProbeCount = 0;
				try {
					checkClients(serverStat);
					checkLowTxnRate(serverStat);
					tcStats.logStats(serverStat);
					sysStats.logStats(serverStat);
				} catch (NotConnectedException e) {
					FATAL.error(e.getMessage());
				}
			} else {
				if(!ClusterWatcherProperties.CHECK_CLUSTER_DOWN){
					LOG.fatal("Cluster down check is not enabled. " +
					"To enable add property. cluster.down.check : true");
					return;
				}
				clusterDownProbeCount++;
				LOG.warn("No ACTIVE-COORDINATOR found !!! Count: "
						+ clusterDownProbeCount + ", Threshold: "
						+ ClusterWatcherProperties.CLUSTER_DOWN_MAX_CHECK);
				check(
						clusterDownProbeCount == ClusterWatcherProperties.CLUSTER_DOWN_MAX_CHECK,
						"Cant find Active-Coordinator. Threshold of "
						+ ClusterWatcherProperties.CLUSTER_DOWN_MAX_CHECK);
			}
			checkMaxActiveServers();
		}
	}

	private ServerStat getActiveCoordinator() {
		ServerStat activeCoordinator = null;
		Iterator<MirrorGroup> iterator = clusterList.iterator();
		while (iterator.hasNext()) {
			MirrorGroup group = iterator.next();
			activeCoordinator = group.getActiveCoordinator();
			if (activeCoordinator != null) {
				return activeCoordinator;
			}
		}
		return activeCoordinator;
	}

	private boolean allServerOnline() {
		Iterator<MirrorGroup> iterator = clusterList.iterator();
		while (iterator.hasNext()) {
			MirrorGroup group = iterator.next();
			if (!group.isAllServerOnline())
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	private void checkMaxActiveServers() {
		Iterator<MirrorGroup> iterator = clusterList.iterator();
		while (iterator.hasNext()) {
			// check for at most one active server per mirror group
			MirrorGroup group = iterator.next();
			int clusterCheckProbeCount = group.getClusterCheckProbeCount();
			check(clusterCheckProbeCount == ClusterWatcherProperties.ONE_ACTIVE_MAX_CHECK,
					"Cluster " + group.toString() + " health failure: "
					+ group.getActiveServerCount()
					+ " active server(s) ");
		}
	}

	private void checkLowTxnRate(ServerStat serverStat) throws NotConnectedException {
		long txr = serverStat.getDsoMbean().getTransactionRate();
		if (txr >= ClusterWatcherProperties.LOW_TXR_THRESHOLD)
			lowTxrProbeCount = 0;
		else {
			lowTxrProbeCount++;
			LOG.warn("Low-Txn-Rate: Curr: " + lowTxrProbeCount + " , MAX: "
					+ ClusterWatcherProperties.LOW_TXR_MAX_CHECK);
		}

		check(lowTxrProbeCount == ClusterWatcherProperties.LOW_TXR_MAX_CHECK,
				"Transaction rate goes below threshold of "
				+ ClusterWatcherProperties.LOW_TXR_THRESHOLD);
	}

	private void checkClients(ServerStat serverStat) throws NotConnectedException {
		int connectedClients = serverStat.getDsoMbean().getClients().length;
		long expectedClients = ClusterWatcherProperties.CLIENT_COUNTS;
		LOG.info("expected " + expectedClients + " got: " + connectedClients
				+ " clients");
		missingClientsProbeCount = (connectedClients < expectedClients ? (missingClientsProbeCount + 1)
				: 0);
		check(missingClientsProbeCount == ClusterWatcherProperties.MISSING_CLIENT_MAX_CHECK,
				"Expecting [" + expectedClients + "] but got ["
				+ connectedClients + "]");
	}

	private void check(boolean condition, String msg) {
		if (condition) {
			FATAL.error(msg);
			Iterator<MirrorGroup> mg  = clusterList.iterator();
			while (mg.hasNext()){
				Iterator<ServerStat> st = mg.next().iterator();
				while (st.hasNext()){
					try {
						st.next().getL2DumperMbean().dumpClusterState();
					} catch (NotConnectedException e) {
						LOG.error(e.getMessage());
					}
				}
			}
			// LOG.info("Resetting all probe counts.");
			// lowTxrProbeCount = 0;
			// clusterDownProbeCount = 0;
			// missingClientsProbeCount = 0;
		}
	}

	public static void main(String[] arg) throws Exception {
		String propertyFile = System.getProperty("test.properties","src/main/resources/test.properties");
		ClusterWatcherProperties.loadProperties(propertyFile);
		new ClusterWatcher().run();
	}

}
