package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.CLASSPATH_SEPARATOR;
import static org.tc.perf.util.SharedConstants.FILELIST;
import static org.tc.perf.util.SharedConstants.HOSTNAME;

import java.util.List;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;

/**
 * @author Himadri Singh
 * 
 * 
 */
public abstract class AbstractL1Work extends AbstractWork {

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(AbstractL1Work.class);
	protected static final String CLIENT_SETUP = "/client-setup";
	protected static final String CLIENT_LOG_LOCATION  = "client-" + HOSTNAME + "-logs";


	protected final List<String> fileList;

	@SuppressWarnings("unchecked")
	public AbstractL1Work(final Configuration configuration) {
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

		String mainClass 		= configuration.getMainClass();
		List<String> arguments 	= configuration.getArguments();
		String location 		= configuration.getLocation();

		String classpath = "";
		for (String file : fileList) {
			classpath += file + CLASSPATH_SEPARATOR;
		}
		List<String> jvmArgs = configuration.getL1_jvmArgs();

		ProcessConfig config = new ProcessConfig(mainClass);
		config.setClasspath(classpath)
		.setArguments(arguments)
		.setLocation(location + CLIENT_SETUP)
		.setRelativeLogDir("../" + CLIENT_LOG_LOCATION)
		.setJvmArgs(jvmArgs)
		.setJavaHome(configuration.getL1_javaHome());
		return config;
	}

}
