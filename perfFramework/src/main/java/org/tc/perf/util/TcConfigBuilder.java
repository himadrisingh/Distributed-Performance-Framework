package org.tc.perf.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;


public class TcConfigBuilder {

	private static final Logger log = Logger.getLogger(TcConfigBuilder.class);

	private static final String TC_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<tc:tc-config xsi:schemaLocation=\"http://www.terracotta.org/config http://www.terracotta.org/schema/terracotta-4.xsd\"\n"
		+ "xmlns:tc=\"http://www.terracotta.org/config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n%s\n</tc:tc-config>";
	private static final String SERVER = "  <server host=\"%s\" name=\"%s\">\n";
	private static final String SERVER_LOG = "\t<data>%s/server-%%h-data</data>\n"
		+ "\t<logs>%s/server-%%h-logs/logs</logs>\n"
		+ "\t<statistics>%s/server-%%h-statistics</statistics>\n";
	private static final String DSO_PORT = "\t<dso-port>9510</dso-port>\n";
	private static final String JMX_PORT = "\t<jmx-port>9520</jmx-port>\n";
	private static final String GROUP_PORT = "\t<l2-group-port>9530</l2-group-port>\n";
	private static final String DGC = "\t<garbage-collection>\n\t\t<enabled>%b</enabled>\n"
		+ "\t\t<verbose>true</verbose>\n\t\t<interval>%d</interval>\n\t</garbage-collection> ";
	// Not needed yet
	//private static final String NAP = "\t<ha>\n\t\t<mode>networked-active-passive</mode>"
	//			+ "\n\t<networked-active-passive>\n\t\t\t<election-time>%d</election-time>\n\t\t</networked-active-passive>";
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
		String server = "  <servers>";

		for (String l2 : config.getL2machines()) {
			server += String.format(SERVER, l2, l2);
			server += String.format(SERVER_LOG, location, location,
					location);
			server += DSO_PORT + JMX_PORT + GROUP_PORT + "\t<dso>\n";
			server += String.format(PERSISTENCE,
					config.getPersistence(), config.isOffheapEnabled(), config
					.getOffheapMaxDataSize());
			server += String.format(DGC, config.isDgcEnabled(), config.getDgcInterval());
			server += "\n\t</dso>\n  </server>\n</servers>\n";
		}
		String client = String.format(CLIENT_LOG, location);
		return String.format(TC_CONFIG, server + client);
	}

	public void createConfig(String location){
		try {
			String configPath = location + "/tc-config.xml";
			FileOutputStream fos = new FileOutputStream(configPath);
			String tcConfig = build();
			log.info(String.format("tc-config.xml created at %s ... \n%s", configPath, tcConfig));
			fos.write(tcConfig.getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Configuration config = new Configuration("src/main/resources/load.properties");
		TcConfigBuilder builder = new TcConfigBuilder(config);
		System.out.println(builder.build());
		builder.createConfig("target");
	}
}

