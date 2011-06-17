package org.tc.perf.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 
 * It builds the terracotta config according to the configuration.
 * 
 * @author Himadri Singh
 */
public class TcConfigBuilder {

	private static final Logger log = Logger.getLogger(TcConfigBuilder.class);

	private static final String TC_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<tc:tc-config xsi:schemaLocation=\"http://www.terracotta.org/config http://www.terracotta.org/schema/terracotta-4.xsd\"\n"
		+ "xmlns:tc=\"http://www.terracotta.org/config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n%s\n</tc:tc-config>";
	private static final String SERVER = "  <server host=\"%s\" name=\"%s\">\n";
	private static final String SERVER_DATA = "\t<data>%s/server/server-%s-%%h-data</data>\n";
	private static final String SERVER_LOG = "\t<logs>%s/server/server-%s-%%h-logs/logs</logs>\n";
	private static final String SERVER_STAT = "\t<statistics>%s/server/server-%s-%%h-statistics</statistics>\n";
	private static final String DSO_PORT = "\t<dso-port>%s</dso-port>\n";
	private static final String JMX_PORT = "\t<jmx-port>%s</jmx-port>\n";
	private static final String GROUP_PORT = "\t<l2-group-port>%s</l2-group-port>\n";
	private static final String DGC = "\t<garbage-collection>\n\t\t<enabled>%b</enabled>\n"
		+ "\t\t<verbose>true</verbose>\n\t\t<interval>%d</interval>\n\t</garbage-collection> ";
	private static final String CLIENT_LOG = "<clients>\n\t<logs>%s/client-%%h-logs/logs</logs>\n</clients>";
	private static final String PERSISTENCE = "\t<persistence>\n\t\t<mode>%s</mode>"
		+ "\n\t\t<offheap>\n\t\t\t<enabled>%b</enabled>\n\t\t\t<maxDataSize>%s</maxDataSize>"
		+ "\n\t\t</offheap>\n\t</persistence>\n";
	private final Configuration config;

	public TcConfigBuilder(final Configuration config) {
		this.config = config;
	}

	private String build() {
		String location = new File(config.getLocation()).getAbsolutePath();
		StringBuilder server = new StringBuilder("  <servers>");

		// List of server name to be used in creating mirror groups.
		List<String> serverList = new ArrayList<String>();
		// Map to get distinct ports on same box.
		Map<String, Integer> servers = new HashMap<String, Integer>();
		for (String l2 : config.getL2machines()) {
			Integer port = servers.get(l2);
			port = (port == null) ? 0 : port;
			servers.put(l2, port + 1);
			String servername = l2 + "-952" + port;
			serverList.add(servername);
			server.append(String.format(SERVER, l2, servername));
			server.append(String.format(SERVER_LOG, location, "952" + port));
			server.append(String.format(SERVER_DATA, location, "952" + port));
			server.append(String.format(SERVER_STAT, location, "952" + port));
			server.append(String.format(DSO_PORT + JMX_PORT + GROUP_PORT, "951"
					+ port, "952" + port, "953" + port));
			server.append("\t<dso>\n");
			server.append(String.format(PERSISTENCE, config.getPersistence(),
					config.isOffheapEnabled(), config.getOffheapMaxDataSize()));
			server.append(String.format(DGC, config.isDgcEnabled(), config
					.getDgcInterval()));
			server.append("\n\t</dso>\n  </server>\n");
		}

		// Creating Mirror Groups
		Iterator<String> itr = serverList.iterator();
		log.info("Servers per Mirror Group: "
				+ config.getServersPerMirrorGroup());
		StringBuilder mirror = new StringBuilder();
		do {
			mirror.append("\t<mirror-group>\n\t\t<members>\n");
			for (int k = 0; k < config.getServersPerMirrorGroup()
			&& itr.hasNext(); k++) {
				mirror.append(String.format("\t\t\t<member>%s</member>\n", itr
						.next()));
			}
			mirror.append("\t\t</members>\n\t</mirror-group>\n");
		} while (itr.hasNext());
		server.append("  <mirror-groups>\n");
		server.append(mirror.toString());
		server.append("  </mirror-groups>\n");
		server.append("</servers>\n");

		// Setting client log location
		String client = String.format(CLIENT_LOG, location);
		return String.format(TC_CONFIG, server.toString() + client);
	}

	/**
	 * Creates the terracotta tc-config.xml as per the configuration and writes
	 * it to file on disk in specified location.
	 * 
	 * @param location
	 *            directory path for tc-config.xml file.
	 */
	public void createConfig(String location) {
		try {
			String configPath = location + "/tc-config.xml";
			FileOutputStream fos = new FileOutputStream(configPath);
			String tcConfig = build();
			log.info(String.format("tc-config.xml created at %s ... \n%s",
					configPath, tcConfig));
			fos.write(tcConfig.getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
