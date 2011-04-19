/**
 * 
 */
package org.tc.perf.work.items;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessExecutor;
import org.tc.perf.util.Configuration;

/**
 * @author gautam
 *
 */
public class StartL1 extends AbstractL1WorkItem {

    private static final long serialVersionUID = 1L;

    public StartL1(final Configuration configuration) {
        super(configuration);
    }

    @Override
    protected void work() {
        ProcessConfig config = getProcessConfig().waitForLogSnippet("Cluster TPS");
        ProcessExecutor exec = new ProcessExecutor(config);
        exec.waitFor(120);
    }

}
