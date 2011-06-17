package org.tc.cluster.watcher;


public class NotConnectedException extends Exception{
	private static final long serialVersionUID = 1L;

	NotConnectedException(String server, Exception e){
		super(String.format("Not Connected to server [%s]. Caused by %s", server, e.getLocalizedMessage()));
	}
}
