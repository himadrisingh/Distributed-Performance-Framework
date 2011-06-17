package org.tc.cluster.watcher.logger;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

import com.tc.management.beans.TCServerInfoMBean;
import com.tc.statistics.StatisticData;
import com.tc.stats.DSOMBean;

public class SystemStatsLogger extends AbstractLogger {
	private static final Logger	SYS_STAT = Logger.getLogger("system_stats");
	private static final Logger LOG		 = Logger.getLogger(SystemStatsLogger.class);
	private static boolean isHeaderLogged = false;

	private static final String MEMORY_USED = "memory used";
	private static final String CPU_USAGE   = "cpu usage";
	private static final String CHANNEL_ID	= "channelID";

	private int getAvgCpu(StatisticData[] stats){
		double cpu = 0.0;
		for (StatisticData s : stats){
			try{
				cpu += Double.parseDouble(s.getData().toString());
			} catch (NumberFormatException e){
				e.printStackTrace();
			}
		}
		return (int) (cpu * 100/stats.length);
	}

	private void logHeaderOnce(Set<ObjectName> keys){
		if (!isHeaderLogged){
			isHeaderLogged = true;
			String message = "L2_CPU , L2MemoryUsed , ";
			for (ObjectName k : keys){
				String client = String.format("ClientID[%s]",k.getKeyProperty(CHANNEL_ID));
				message += String.format("%s_CPU , %sMemoryUsed ,", client, client);
			}
			SYS_STAT.debug(message);
		}
	}

	@Override
	public void logStats(ServerStat stat) throws NotConnectedException {
		try {
			DSOMBean dso = stat.getDsoMbean();
			TCServerInfoMBean info = stat.getInfoBean();
			Map l2Stats = info.getStatistics();
			Map<ObjectName, Map> l1Stats = dso.getL1Statistics();

			// TODO: Check for new clients connected
			logHeaderOnce(l1Stats.keySet());

			StringBuilder data = new StringBuilder();
			data.append(getAvgCpu((StatisticData[])l2Stats.get(CPU_USAGE)) + "%").append(SEP)
			.append(l2Stats.get(MEMORY_USED)).append(SEP);

			Iterator<ObjectName> itr = l1Stats.keySet().iterator();
			while (itr.hasNext()){
				Map l1StatsMap = l1Stats.get(itr.next());
				data.append(getAvgCpu((StatisticData[])l1StatsMap.get(CPU_USAGE)) + "%")
				.append(SEP).append(l1StatsMap.get(MEMORY_USED)).append(SEP);
			}
			SYS_STAT.debug(data.toString());
		}
		catch (UndeclaredThrowableException e){
			LOG.error(e.getLocalizedMessage());
		}
	}

}
