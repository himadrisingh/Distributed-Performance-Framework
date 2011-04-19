package org.tc.perf;

import static org.tc.perf.util.SharedConstants.HOSTNAME;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;

public class BootStrapClass {

    private static final Logger log = Logger.getLogger(BootStrapClass.class);
    private static final String SLAVE = "slave";
    private static final String MASTER = "master";
    private static final String tcConfigSample = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "  <tc:tc-config xsi:schemaLocation=\"http://www.terracotta.org/config http://www.terracotta.org/schema/terracotta-4.xsd\">\n"
        + "    <servers>\n"
        + "      <server host=\"%i\" name=\"%i\">\n"
        + "        <dso-port>8510</dso-port>\n" + "      <server>\n"
        + "    <servers>\n"
        + "  </tc:tc-config>\n";

    public static final void runMaster(final String arg){
        log.info(String.format("Starting performance framework master on %s ...", HOSTNAME));
        Configuration config = new Configuration(arg);
        Master master = new Master(config);
        master.run();
    }


    public static final void runSlave(){
        log.info(String.format("Starting performance framework slave process on %s...", HOSTNAME));
        Slave slave = new Slave();
        slave.poll();
    }

    public static final void printHelp(){
        log
        .info("\nWelcome to Terracotta distributed perf framework.\nTo use this we need to start a Terracotta server at port 8510 (other than 9510) for framework.\n"
                + "\nSample tc-config to start tc-server at port 8510: \n\n"
                + tcConfigSample
                + "\nProvide the tc config url as system properties \"fw.tc.config\" to master/slave process to connect to tc-server.\n eg: -Dfw.tc.config=<tc-server-hostname>:<dso-port>\n"
                + "\nTo run slave process:\n \tjava -jar -Dfw.tc.config=<tc-server-hostname>:<dso-port> perfFramework.jar slave "
                + "\n\nTo run master process to load the test:\n \tjava -jar -Dfw.tc.config=<tc-server-hostname>:<dso-port> perfFramework.jar master <test-configuration-file>");
    }

    public static void main(final String[] args) {
        if(args.length > 0){
            if (args[0].equalsIgnoreCase(MASTER) && args.length > 1)
                runMaster(args[1]);
            else if (args[0].equalsIgnoreCase(SLAVE))
                runSlave();
            else printHelp();
        }
        else printHelp();
    }
}
