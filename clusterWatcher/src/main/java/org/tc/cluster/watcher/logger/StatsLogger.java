package org.tc.cluster.watcher.logger;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

import com.tc.stats.DSOMBean;

public class StatsLogger extends AbstractLogger {
	private static final Logger	CSV = Logger.getLogger("csv");

	private static final String Header = "Time , LiveObjectCount , WriteTxnRate , L2DiskFaultRate , ObjectFaultRate , " +
	"ObjectFlushRate , OffheapFaultRate , OffheapFlushRate , Broadcast , OffheapMap , OffheapObject , PendingTxns";

	public StatsLogger() {
		CSV.debug(Header);
	}

	@Override
	public void logStats(ServerStat serverStat){
		DSOMBean bean;
		try {
			bean = serverStat.getDsoMbean();
			StringBuilder builder = new StringBuilder(getTime());
			builder.append(SEP)
			.append(bean.getLiveObjectCount()).append(SEP)
			.append(bean.getTransactionRate()).append(SEP)
			.append(bean.getL2DiskFaultRate()).append(SEP)
			.append(bean.getObjectFaultRate()).append(SEP)
			.append(bean.getObjectFlushRate()).append(SEP)
			.append(bean.getOffHeapFaultRate()).append(SEP)
			.append(bean.getOffHeapFlushRate()).append(SEP)
			.append(bean.getBroadcastRate()).append(SEP)
			.append(bean.getOffheapMapAllocatedMemory()).append(SEP)
			.append(bean.getOffheapObjectAllocatedMemory()).append(SEP)
			.append(bean.getPendingTransactionsCount());
			CSV.debug(builder.toString());
		} catch (NotConnectedException e) {
			//
		}
	}

}
