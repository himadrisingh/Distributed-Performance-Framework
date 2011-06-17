package org.tc.perf.work.items;

import java.io.Serializable;

import org.tc.perf.process.ProcessState;

/**
 * Simple interface that describes a unique Work item for the framework
 * 
 * This interface is an abstraction that describes a unique task that the
 * framework can execute. "Work" objects are placed in the queue for an agent
 * node process. Agent nodes process each work item by calling doWork()
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
	 * Gets the current state of work
	 * 
	 * @return @ProcessState defining the process state
	 */
	public ProcessState getState();

}