package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.CLASSPATH_SEPARATOR;
import static org.tc.perf.util.SharedConstants.FILELIST;
import static org.tc.perf.util.SharedConstants.HOSTNAME;

import java.util.List;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;

public abstract class AbstractLoadWork extends AbstractWork {

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(AbstractLoadWork.class);
	protected static final String LOAD_SETUP = "/load-setup";
	protected static final String LOAD_LOG_LOCATION  = "load-" + HOSTNAME + "-logs";

	protected final List<String> fileList;

	@SuppressWarnings("unchecked")
	public AbstractLoadWork(final Configuration configuration) {
		super(configuration);

		Object files = getDataCache().get(FILELIST).getValue();
		this.fileList = (files instanceof List<?>) ? (List<String>) files : null;
		if (fileList == null) {
			throw new UnsupportedOperationException(
					"Expected to find a List<String> in file list cache but found "
					+ files.getClass() + " instead.");
		}
	}

	protected ProcessConfig getProcessConfig(){

		String mainClass 		= configuration.getLoadMainClass();
		List<String> arguments 	= configuration.getLoadArguments();
		String location 		= configuration.getLocation();

		String classpath = "";
		for (String file : fileList) {
			classpath += file + CLASSPATH_SEPARATOR;
		}
		List<String> jvmArgs = configuration.getLoad_jvmArgs();

		ProcessConfig config = new ProcessConfig(mainClass);
		config.setClasspath(classpath)
		.setArguments(arguments)
		.setLocation(location + LOAD_SETUP)
		.setRelativeLogDir("../" + LOAD_LOG_LOCATION)
		.setJvmArgs(jvmArgs);
		return config;
	}

}
