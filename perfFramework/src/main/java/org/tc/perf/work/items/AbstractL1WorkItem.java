package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.CLASSPATH_SEPARATOR;
import static org.tc.perf.util.SharedConstants.CLIENT_LOG_LOCATION;
import static org.tc.perf.util.SharedConstants.CLIENT_SETUP;
import static org.tc.perf.util.SharedConstants.FILELIST;

import java.util.List;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;

public abstract class AbstractL1WorkItem extends AbstractWork {

    private static final long serialVersionUID = 1L;
    protected static final Logger log = Logger.getLogger(AbstractL1WorkItem.class);

    protected final List<String> fileList;

    @SuppressWarnings("unchecked")
    public AbstractL1WorkItem(final Configuration configuration) {
        super(configuration);

        Object files = getTestCache().get(FILELIST).getValue();
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
        .setJvmArgs(jvmArgs);
        return config;
    }

}
