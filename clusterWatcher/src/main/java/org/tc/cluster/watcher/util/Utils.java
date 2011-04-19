package org.tc.cluster.watcher.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.xpath.XPathConstants;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.MirrorGroup;
import org.tc.cluster.watcher.ServerStat;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Utils {
	static final Logger LOG    = Logger.getLogger(Utils.class);

	public static void merge(InputStream in, OutputStream out) {
		StreamCopier sc = new StreamCopier(in, out);
		sc.start();
	}

	public static ArrayList<MirrorGroup> getHostAndJMXStringForGroups(String url) {
		ArrayList<MirrorGroup> cluster	= new ArrayList<MirrorGroup>();
		HashMap<String, ServerStat> serverList = new HashMap<String, ServerStat>();
		try {
			XPathReader reader = new XPathReader(url);
			NodeList servers = (NodeList) reader.read("//servers/server" ,
					XPathConstants.NODESET);

			// Get available TC servers with jmx-port
			for (int i = 0; i < servers.getLength(); i++) {
				Node server 			= servers.item(i);
				String host 			= server.getAttributes().getNamedItem("host").getNodeValue();
				String name 			= server.getAttributes().getNamedItem("name").getNodeValue();
				NodeList serverInfo 	= server.getChildNodes();
				int port				= 9520;

				for (int j = 0; j < serverInfo.getLength(); j++) {
					Node info = serverInfo.item(j);
					if ("jmx-port".equals(info.getNodeName())) {
						port = Integer.parseInt(info.getFirstChild().getNodeValue());
					}
				}
				ServerStat serverStat 	= new ServerStat(host, port);
				serverList.put(name, serverStat);
				if (LOG.isDebugEnabled())
					LOG.debug("Server-" + i + ": " + name + " (" + host + ":"+ port + ")");
			}

			// Get mirror groups and create array of server in each mirror grps
			NodeList mirrorGroups = (NodeList) reader.read("//servers/mirror-groups/mirror-group/members",
					XPathConstants.NODESET);
			for (int i = 0; i < mirrorGroups.getLength(); i++ ){
				MirrorGroup group	= new MirrorGroup();
				NodeList mirrorGrp = mirrorGroups.item(i).getChildNodes();
				for (int j = 0; j < mirrorGrp.getLength(); j++){
					Node member = mirrorGrp.item(j);
					if ( "member".equals(member.getNodeName())){
						ServerStat server = serverList.get(member.getFirstChild().getNodeValue());
						if (LOG.isDebugEnabled())
							LOG.debug("Mirror Group - " + i + ": " + server);
						group.addMember(server);
					}
				}
				cluster.add(group);
			}

			if ( mirrorGroups.getLength() == 0 ){
				MirrorGroup single = new MirrorGroup();
				Iterator<Entry<String, ServerStat>> iterator = serverList.entrySet().iterator();
				while (iterator.hasNext()){
					single.addMember(iterator.next().getValue());
				}
				cluster.add(single);
			}
		} 	catch (Exception e) {
			cluster = null;
			LOG.error("Error Connecting..." + e.getMessage());
		}
		return cluster;
	}


	public static ArrayList<MirrorGroup> getHostAndJMXStringFromTcConfig(String tcConfig) {
		File file = new File(tcConfig);
		if (file.exists()) {
			LOG.info("Using tc-config.xml file: " + tcConfig);
			return getHostAndJMXStringForGroups(file.toURI().toString());
		} else {
			String[] servers = tcConfig.split(",");
			for (String server : servers) {
				LOG.info("Loading config from server: " + server);
				ArrayList<MirrorGroup> result = getHostAndJMXStringForGroups("http://" + server + "/config");
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	public static void dump() {
		/*		try {
			//			Runtime.getRuntime().exec("killall -3 java");
		} catch (IOException e) {
			e.printStackTrace();
		}
		 */	}

}
