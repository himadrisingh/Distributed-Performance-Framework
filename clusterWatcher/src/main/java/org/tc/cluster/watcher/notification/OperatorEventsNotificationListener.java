package org.tc.cluster.watcher.notification;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;

public class OperatorEventsNotificationListener implements NotificationListener{

	private static Logger eventLog = Logger.getLogger("eventLogger");

	public void handleNotification(Notification notification, Object handback) {
		eventLog.debug(notification.getSource());
	}

}
