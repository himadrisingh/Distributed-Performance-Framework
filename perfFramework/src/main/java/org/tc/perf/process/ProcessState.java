package org.tc.perf.process;

import java.io.Serializable;

/**
 * ProcessState defines the state of a process started. this can be following
 * states <li>NOT_STARTED is the default state</li> <li>INITIALIZED is set when
 * process executed in ProcessThread</li> <li>STARTED is set when required log
 * snippet is found, set by StreamCopier</li> <li>FINISHED is set when process
 * finishes executing in ProcessThread</li> <li>FAILED is set when failed
 * abnormally</li> <li>TIMEOUT is set when process failed to start within
 * specified timeout period</li>
 * 
 * @author Himadri Singh
 */
public class ProcessState implements Serializable {
	private static final long serialVersionUID = 1L;

	private enum State {
		NOT_STARTED, INITIALIZED, STARTED, FINISHED, FAILED, TIMEOUT;
	}

	private State state = State.NOT_STARTED;
	private String failureReason = "unknown";

	public boolean isNotStarted() {
		return State.NOT_STARTED.equals(state);
	}

	public boolean isInitialized() {
		return State.INITIALIZED.equals(state);
	}

	public boolean isStarted() {
		return State.STARTED.equals(state);
	}

	public boolean isFinished() {
		return State.FINISHED.equals(state);
	}

	public boolean isFailed() {
		return State.FAILED.equals(state);
	}

	public boolean isTimeout() {
		return State.TIMEOUT.equals(state);
	}

	public synchronized void markInitialized() {
		this.state = State.INITIALIZED;
	}

	public synchronized void markStarted() {
		this.state = State.STARTED;
	}

	public synchronized void markFinished() {
		this.state = State.FINISHED;
	}

	public synchronized void markFailed() {
		this.state = State.FAILED;
	}

	public synchronized void markTimeout() {
		this.state = State.TIMEOUT;
	}

	public synchronized void setFailureReason(String reason) {
		failureReason = reason;
	}

	public String getFailureReason() {
		return failureReason;
	}

	@Override
	public String toString() {
		if (State.FAILED.equals(state))
			return "Process State: " + state + ". Failure Reason: "
			+ failureReason;
		return "Process State: " + state;
	}
}
