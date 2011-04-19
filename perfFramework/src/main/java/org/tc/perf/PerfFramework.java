package org.tc.perf;
import static org.tc.perf.util.SharedConstants.NOTIFICATIONS;
import static org.tc.perf.util.SharedConstants.TC_CONFIG_URL;
import static org.tc.perf.util.SharedConstants.TEST_CACHE;

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;

import org.tc.perf.util.CacheGenerator;
import org.tc.perf.util.FileLoader;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;

public abstract class PerfFramework {

    protected static final ClusteringToolkit toolkit = new TerracottaClient(TC_CONFIG_URL).getToolkit();

    protected final FileLoader loader;
    protected final Cache notification;
    protected final Cache test;

    public PerfFramework() {
        this.notification = CacheGenerator.getCache(NOTIFICATIONS);
        this.test = CacheGenerator.getCache(TEST_CACHE);
        this.loader = new FileLoader(test);
    }

    protected void sleep(final long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void reset(){
        test.removeAll();
        notification.removeAll();
    }

}
