package org.tc.cluster.watcher;

public class NotConnectedException extends Exception{
	private static final long serialVersionUID = 1L;

	NotConnectedException(){
		super("Not Connected to cluster");
	}
}
