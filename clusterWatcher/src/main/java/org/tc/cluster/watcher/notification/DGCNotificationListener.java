package org.tc.cluster.watcher.notification;

import java.text.SimpleDateFormat;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;

import com.tc.objectserver.api.GCStats;

public class DGCNotificationListener implements NotificationListener {

	private static final Logger LOG = Logger.getLogger(DGCNotificationListener.class);
	private static final Logger DGC = Logger.getLogger("dgc");
	private static final String GC_STATUS_UPDATE 	= "dso.gc.status.update";
	private static final String CLIENT_ATTACHED 	= "dso.client.attached";
	private static final String CLIENT_DETACHED		= "dso.client.detached";
	private static final String SEP = " , ";
	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static final String GC_COMPLETE      = "COMPLETE";

	public DGCNotificationListener() {
		DGC.debug("Iteration , StartTime , BeginObjectCount , ActualGarbageCount , EndObjectCount , MarkStageTime , PausedStageTime , ElapsedTime");
	}

	private void checkDGC(Notification notification){
		GCStats stat = (GCStats) notification.getSource();
		if (GC_COMPLETE.equals(stat.getStatus())){
			DGC.debug(statToString(stat));
		}
		LOG.info(stat);
	}

	private String statToString(GCStats stat){
		return stat.getIteration() + SEP + format.format(stat.getStartTime()) + SEP
		+ stat.getBeginObjectCount() + SEP
		+ stat.getActualGarbageCount() + SEP
		+ stat.getEndObjectCount() + SEP + stat.getMarkStageTime()
		+ SEP + stat.getPausedStageTime() + SEP
		+ stat.getElapsedTime();
	}

	public void handleNotification(Notification notification, Object handback) {
		//		System.out.println(notification.getType());
		if (GC_STATUS_UPDATE.equals(notification.getType())){
			checkDGC(notification);
		}
	}

}
