package org.tc.perf;

import static org.tc.perf.util.SharedConstants.HOSTNAME;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.work.items.Work;

public class Slave extends PerfFramework {

    private static final Logger log = Logger.getLogger(Slave.class);

    public void poll() {
        log.info("Slave ready for work from master.");
        while (true) {
            Work work = getWork();
            if (work == null) {
                sleep(1000);
            } else {
                work.doWork();
                updateWork(work);
            }
        }
    }

    /**
     * Updates the Work for this host in the cache with the new state
     * 
     * @param work
     *            The updated Work object
     */
    private void updateWork(final Work work) {
        notification.put((new Element(HOSTNAME, work)));
    }

    private Work getWork() {
        Element e = notification.get(HOSTNAME);
        return (e == null) ? null : (Work) e.getValue();
    }

}
