package org.tc.cluster.watcher.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

public abstract class AbstractLogger {

	protected static final String SEP = " , ";
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected String getTime(){
		return format.format(new Date());
	}

	abstract public void logStats(ServerStat stat) throws NotConnectedException;
}
