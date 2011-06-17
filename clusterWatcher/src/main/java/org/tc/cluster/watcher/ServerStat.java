package org.tc.cluster.watcher;

import java.io.IOException;
import java.net.ConnectException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.util.JMXConnectorProxy;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.stats.DSOMBean;

public class ServerStat {

	static final Logger LOG = Logger.getLogger(ServerStat.class);

	private static final String   SERVER_MBEAN_NAME = "org.terracotta.internal:type=Terracotta Server,name=Terracotta Server";
	private static final String   DSO_MBEAN_NAME    = "org.terracotta:type=Terracotta Server,name=DSO";
	private static final String   L2_DUMPER			= "org.terracotta.internal:type=Terracotta Server,name=L2Dumper";
	private static final String	  OPS_EVENT			= "org.terracotta:type=TC Operator Events,name=Terracotta Operator Events Bean";

	//	private static final String   ACTIVE			= "Active";
	//	private static final String   SERVER_STATE		= "State";
	//	private static final String   PASSIVE_STANDBY	= "PassiveStandby";

	public String host;
	public int port;
	private JMXConnectorProxy jmxProxy;
	private MBeanServerConnection mbsc;

	private DSOMBean dsoMBean;
	private TCServerInfoMBean infoMBean;
	private L2DumperMBean l2DumperMBean;

	public ServerStat(String host, int port) {
		if (host.equals("%i"))
			host = "localhost";
		this.host = host;
		this.port = port;
		try {
			init();
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void init() throws NotConnectedException {
		if (jmxProxy != null) {
			try {
				jmxProxy.close();
			} catch (IOException e) {
				// ignore
			}
		}
		jmxProxy = new JMXConnectorProxy(host, port);
		try {
			mbsc = jmxProxy.getMBeanServerConnection();
			ObjectName serverObjectName = new ObjectName(SERVER_MBEAN_NAME);
			ObjectName dsoObjectName = new ObjectName(DSO_MBEAN_NAME);
			ObjectName l2dumperObjectName = new ObjectName(L2_DUMPER);

			dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
					dsoObjectName, DSOMBean.class, false);

			infoMBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
					serverObjectName, TCServerInfoMBean.class, false);

			l2DumperMBean = (L2DumperMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
					l2dumperObjectName, L2DumperMBean.class, false);

		}
		catch (ConnectException ce){
			LOG.warn(this + " : Connection Refused. Probably server crashed.");
			throw new NotConnectedException(host + ":" + port, ce);
		}
		catch (Exception e) {
			throw new NotConnectedException(host + ":" + port, e);
		}
	}

	public boolean isConnected() {
		try {
			mbsc.getDefaultDomain();
		} catch (Exception e) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public TCServerInfoMBean getInfoBean() throws NotConnectedException{
		if (!isConnected()) {
			init();
		}
		return infoMBean;
	}

	public DSOMBean getDsoMbean() throws NotConnectedException{
		if (!isConnected()) {
			init();
		}
		return dsoMBean;
	}

	public L2DumperMBean getL2DumperMbean() throws NotConnectedException{
		if (!isConnected()) {
			init();
		}
		return l2DumperMBean;
	}

	public void registerDsoNotificationListener(NotificationListener listener)
	throws InstanceNotFoundException, IOException,
	MalformedObjectNameException, NullPointerException {
		ObjectName dsoObjectName = new ObjectName(DSO_MBEAN_NAME);
		mbsc.addNotificationListener(dsoObjectName, listener, null, null);
		LOG.info("Added DSO Notification listener...");
	}

	public void registerEventNotificationListener(NotificationListener listener)
	throws InstanceNotFoundException, IOException,
	MalformedObjectNameException, NullPointerException {
		ObjectName eventListenerObjectName = new ObjectName(OPS_EVENT);
		mbsc.addNotificationListener(eventListenerObjectName, listener, null,
				null);
		LOG.info("Added Event Nofitication Listener...");
	}

	@Override
	public String toString() {
		return "[" + host + ":" + port + "]";
	}
}

