package org.tc.perf.work.items;

import org.tc.perf.util.Configuration;

public class InitialCleanup extends AbstractCleanupWork {

    private static final long serialVersionUID = 1L;

    public InitialCleanup(final Configuration configuration) {
        super(configuration);
    }

    @Override
    protected void work() {
        deleteDir(configuration.getLocation());
    }

}
