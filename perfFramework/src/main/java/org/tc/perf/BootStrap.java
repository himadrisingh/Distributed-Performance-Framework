package org.tc.perf;

import static org.tc.perf.util.SharedConstants.HOSTNAME;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;

/**
 * BootStrap class is used <li>to start Master</li> <li>to start Agent</li> <li>
 * to list running tests</li> <li>to kill a running test</li>
 * 
 * @author Himadri Singh
 * 
 */
public class BootStrap {

	private static final Logger log = Logger.getLogger(BootStrap.class);

	private static enum CMD {
		AGENT, MASTER, LIST, KILL
	};

	private static final String tcConfigSample = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "  <tc:tc-config xsi:schemaLocation=\"http://www.terracotta.org/config http://www.terracotta.org/schema/terracotta-4.xsd\">\n"
		+ "    <servers>\n"
		+ "      <server host=\"%i\" name=\"%i\">\n"
		+ "        <dso-port>8510</dso-port>\n"
		+ "      <server>\n"
		+ "    <servers>\n" + "  </tc:tc-config>\n";

	/**
	 * Start Master process using the configuration provided.
	 * 
	 * @param arg
	 *            configuration file path to be used to load the test.
	 */

	public static final void runMaster(final String arg) {
		if (arg == null) {
			printHelp();
			return;
		}
		log.info(String.format(
				"Starting performance framework master on %s ...", HOSTNAME));
		Configuration config = new Configuration(arg);
		Master master = new Master(config);
		master.run();
	}

	/**
	 * Start the agent. It will connect to the terracotta server and ready for
	 * any work provided to it.
	 */
	public static final void runAgent() {
		log.info(String.format("Starting Test Framework agent on %s...",
				HOSTNAME));
		Agent agent = new Agent();
		agent.poll();
	}

	/**
	 * prints the help message.
	 */
	public static final void printHelp() {
		log.info("Welcome to Terracotta distributed test framework.");
		log
		.info("Need to start a Terracotta server at port 8510 for framework.");
		log
		.info("Provide the tc config url as system properties \"fw.tc.config\" to master/agent process to connect to tc-server.");
		log.info(" eg: -Dfw.tc.config=<tc-server-hostname>:<dso-port>\n");
		log
		.info("Usage: \t"
				+ BootStrap.class.getName()
				+ "  [ MASTER <test-configuration-file>  |  AGENT  |  LIST  |  KILL <test-id> ] ");
		log.info("MASTER \t\tStarts master process to load the test.");
		log.info("\t\t\tIt needs <test-configuration-file> as argument.");
		log.info("AGENT \t\tStarts agent process. ");
		log
		.info("LIST \t\tLists the tests running (unique ids) in the framework");
		log.info("\t\t\talong with the machines being used.");
		log.info("KILL \t\tKills the test. Needs the test unique id.\n");
		log
		.info("Sample tc-config to start terracotta-server at port 8510:\n\n"
				+ tcConfigSample);
	}

	/**
	 * lists the running tests in the test framework.
	 */
	public static final void listTests() {
		Helper hlp = new Helper();
		hlp.listRunningTests();
	}

	/**
	 * Kills a running tests. It needs test-unique-id which can be listed using
	 * listsTests
	 * 
	 * @param uniqueId
	 *            test unique id.
	 */
	public static final void killTest(String uniqueId) {
		if (uniqueId == null) {
			printHelp();
			return;
		}
		Helper hlp = new Helper();
		hlp.killTest(uniqueId.trim());
	}

	public static void main(final String[] args) {
		if (args.length > 0) {
			CMD cmd = CMD.valueOf(args[0]);

			switch (cmd) {
			case MASTER:
				runMaster(args[1]);
				break;
			case AGENT:
				runAgent();
				break;
			case KILL:
				killTest(args[1]);
				break;
			case LIST:
				listTests();
				break;
			default:
				printHelp();
			}
		} else
			printHelp();
	}
}
