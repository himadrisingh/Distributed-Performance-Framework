package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.TC_INSTALL_DIR;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.ehcache.Element;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessExecutor;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;

/**
 * @author Himadri Singh
 * 
 * 
 */
public abstract class AbstractL2Work extends AbstractWork {

	private static final long serialVersionUID = 1L;
	protected static final String TC_MAIN_CLASS = "com.tc.server.TCServerMain";
	protected static final String TC_STOP_MAIN_CLASS = "com.tc.admin.TCStop";
	protected static final String TC_CLASSPATH = "/lib/tc.jar";
	protected static final String TC_START_LOG = "Terracotta Server instance has started up";
	protected static final String TC_STOP_LOG = "stopping it";

	protected List<Pattern> patterns = new ArrayList<Pattern>();

	public AbstractL2Work(Configuration configuration) {
		super(configuration);

		for (String p : configuration.getLogRegex())
			patterns.add(Pattern.compile(p));
	}

	protected String getTcInstallDir() throws NullPointerException {
		Element e = getDataCache().get(TC_INSTALL_DIR);
		if (e == null) {
			throw new NullPointerException("TC_INSTALL_DIR is null");
		}
		return (String) e.getValue();
	}

	@Override
	public ProcessState work() {
		try{
			ProcessConfig config = getProcessConfig();
			ProcessExecutor server = new ProcessExecutor(config);
			return server.executeAndWaitFor(120);
		} catch (Exception e){
			ProcessState state = new ProcessState();
			state.markFailed();
			state.setFailureReason(e.getMessage());
			return state;
		}
	}

	protected abstract ProcessConfig getProcessConfig();
}
