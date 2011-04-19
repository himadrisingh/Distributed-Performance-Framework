package org.tc.perf.work.items;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.tc.perf.util.Configuration;

public abstract class AbstractCleanupWork extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public AbstractCleanupWork(Configuration configuration) {
		super(configuration);
	}

	protected void deleteDir(String location){
		try {
			File dirs = new File(location);
			FileUtils.deleteDirectory(dirs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
