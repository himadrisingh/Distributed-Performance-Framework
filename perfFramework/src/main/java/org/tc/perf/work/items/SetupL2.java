/**
 * 
 */
package org.tc.perf.work.items;

import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;
import org.tc.perf.util.TcConfigBuilder;

/**
 * Work item to set up the L2.
 * 
 * Sets up the L2 by downloading and extracting the kit and building the
 * tc-config.xml
 * 
 */
public class SetupL2 extends AbstractL2WorkItem {

    private static final long serialVersionUID = 1L;

    public SetupL2(final Configuration configuration) {
        super(configuration);
    }

    @Override
    public void work() {
        FileLoader loader = new FileLoader(getTestCache());
        String kitPath = loader.downloadExtractKit(configuration.getLocation());
        setTcInstallDir(kitPath);
        downloadLicense(loader, kitPath);

        TcConfigBuilder builder = new TcConfigBuilder(configuration);
        builder.createConfig(kitPath);
    }
}
