/**
 * 
 */
package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.SERVER_LOG_LOCATION;
import static org.tc.perf.util.SharedConstants.TC_CLASSPATH;
import static org.tc.perf.util.SharedConstants.TC_MAIN_CLASS;

import java.util.ArrayList;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessExecutor;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.SharedConstants;

/**
 * Work item to start an L2
 * 
 * Builds the classpath and starts the L2 server.
 * 
 */
public class StartL2 extends AbstractL2WorkItem {

    private static final long serialVersionUID = 1L;

    public StartL2(final Configuration configuration) {
        super(configuration);
    }

    protected ProcessConfig getProcessConfig(){
        String TC_INSTALL_DIR = getTcInstallDir();
        log.info("TC_INSTALL_DIR: " + TC_INSTALL_DIR);

        ArrayList<String> jvmArgs = new ArrayList<String>(configuration.getL2_jvmArgs());
        jvmArgs.add("-Dtc.install-root=" + TC_INSTALL_DIR);

        ArrayList<String> args = new ArrayList<String>();
        args.add("-f");
        args.add("tc-config.xml");

        ProcessConfig config = new ProcessConfig(TC_MAIN_CLASS);
        config.setClasspath(TC_INSTALL_DIR + TC_CLASSPATH)
        .setArguments(args)
        .setLocation(TC_INSTALL_DIR)
        .setJvmArgs(jvmArgs)
        .setRelativeLogDir("../" + SERVER_LOG_LOCATION);

        return config;
    }

    @Override
    protected void work() {
        ProcessConfig config = getProcessConfig().waitForLogSnippet(SharedConstants.TC_START_LOG);
        ProcessExecutor server = new ProcessExecutor(config);
        boolean bool = server.waitFor(120);
        if (bool)
            log.info("Server Started successfuly.");
        else
            log.info("Error starting Server");
    }

}
