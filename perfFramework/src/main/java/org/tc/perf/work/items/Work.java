package org.tc.perf.work.items;

import java.io.Serializable;

/**
 * Simple interface that describes a unique Work item for the framework
 * 
 * This interface is an abstraction that describes a unique task that the
 * framework can execute. "Work" objects are placed in the queue for a slave
 * node process. Slave nodes process each work item by calling doWork()
 * 
 * Work items are not intended to be re-usable. Once a work item finishes,
 * successfully or otherwise, the doWork() method should be a no-op.
 * 
 * Extends Serializable so that all work items are automatically Serializable
 * ready to cache.
 */

public interface Work extends Serializable {

    /**
     * Do some work. Once.
     * 
     * Do some work once, subsequent invocations should be a no-op.
     */
    public void doWork();

    /**
     * Check if the work is complete
     * 
     * @return True if the work is complete.
     */
    public boolean isWorkDone();

}