package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.SERVER_LOG_LOCATION;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;

public class CleanupL2 extends AbstractL2WorkItem {

    private static final long serialVersionUID = 1L;

    public CleanupL2(final Configuration configuration) {
        super(configuration);
    }

    @Override
    protected void work() {
        FileLoader loader = new FileLoader(getTestCache());
        String clientLogLocation = configuration.getLocation() + SERVER_LOG_LOCATION;
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(Pattern.compile(".*log"));

        List<File> dirs = new ArrayList<File>();
        dirs.add(new File(clientLogLocation));
        dirs.add(new File(clientLogLocation + "/logs"));

        loader.gzipAndUpload(dirs, patterns,SERVER_LOG_LOCATION + ".tar.gz");
        log.info("Test finished. Ready for work...");
    }

}
