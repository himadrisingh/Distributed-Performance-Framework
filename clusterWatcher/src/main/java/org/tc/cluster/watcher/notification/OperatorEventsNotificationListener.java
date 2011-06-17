package org.tc.cluster.watcher.notification;

import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

import com.tc.operatorevent.TerracottaOperatorEvent;

public class OperatorEventsNotificationListener implements NotificationListener{

	private static Logger eventLog = Logger.getLogger("eventLogger");
	public OperatorEventsNotificationListener(ServerStat server) {
		try {
			List<TerracottaOperatorEvent> events = server.getDsoMbean().getOperatorEvents();
			for (TerracottaOperatorEvent event : events){
				eventLog.debug(event);
			}
		} catch (NotConnectedException e) {
			//
		}
	}

	public void handleNotification(Notification notification, Object handback) {
		eventLog.debug(notification.getSource());
	}

}
