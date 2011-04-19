package org.tc.cluster.watcher;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class MirrorGroup {
	private static final Logger LOG = Logger.getLogger(MirrorGroup.class);
	private static final String   ACTIVE_COORDINATOR = "ACTIVE-COORDINATOR";

	private int clusterCheckProbeCount = 0;
	private final ArrayList<ServerStat> members	= new ArrayList<ServerStat>();

	public void addMember (ServerStat server){
		members.add(server);
	}

	public Iterator<ServerStat> iterator(){
		return members.iterator();
	}

	public int getActiveServerCount(){
		int count	= 0;
		Iterator<ServerStat> iterator = iterator();
		ServerStat server;
		while (iterator.hasNext()){
			server = iterator.next();
			try {
				if (server.getInfoBean().isActive())
					count++;
			} catch (NotConnectedException e) {
				LOG.error(e.getMessage());
			}
		}
		LOG.debug("No. of Active Server(s) in " + this.toString() + " : "+count);
		clusterCheckProbeCount = ( count > 1 )? (clusterCheckProbeCount + 1) : 0;
		return count;
	}

	public int getClusterCheckProbeCount() {
		return clusterCheckProbeCount;
	}

	public boolean isAllServerOnline(){
		Iterator<ServerStat> iterator = iterator();
		ServerStat server 	= null;
		while (iterator.hasNext()){
			server = iterator.next();
			if (!server.isConnected())
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public ServerStat getActiveCoordinator(){
		Iterator<ServerStat> iterator = iterator();
		ServerStat activeCoordinator = null;
		ServerStat server 	= null;
		while (iterator.hasNext()){
			server = iterator.next();
			try {
				if (ACTIVE_COORDINATOR.equals(server.getInfoBean().getState())) {
					if (activeCoordinator == null)
						activeCoordinator = server;
					else
						LOG.error("Multiple Active Server in a Mirror Group.");
				}
			} catch (NotConnectedException e) {
				LOG.error(e.getMessage());
			}
		}
		return activeCoordinator;
	}

	@Override
	public String toString(){
		StringBuilder str = new StringBuilder().append("Mirror-Group = [ ");
		Iterator<ServerStat> iterator = iterator();
		ServerStat server 	= null;
		while (iterator.hasNext()){
			server = iterator.next();
			if (server != null)
				str.append(server.host).append(":")
				.append(server.port).append(" ");
		}
		str.append(" ]");
		return str.toString();
	}

}
