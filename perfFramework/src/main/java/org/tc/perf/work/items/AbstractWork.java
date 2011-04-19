package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.TEST_CACHE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;
import org.tc.perf.util.CacheGenerator;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileLoader;

/**
 * Abstract base class for work
 * 
 * Base class for "Work" implementations. Maintains the configuration and
 * completion state of the work at hand.
 * 
 * @see Work for more details.
 */

public abstract class AbstractWork implements Work {

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(AbstractWork.class);

    protected final Configuration configuration;
    private boolean workDone = Boolean.FALSE;

    public AbstractWork(final Configuration configuration) {
        this.configuration = configuration;
    }

    protected static Cache getTestCache(){
        return CacheGenerator.getCache(TEST_CACHE);
    }

    /**
     * Do some work
     * 
     * Delegates the work implementation to sub classes and sets the work flag
     * to true after completion.
     */
    public void doWork() {
        if (!isWorkDone()) {
            try {
                log.info("Starting process for " + this.getClass().getName());
                work();
            } finally {
                log.info("Completed " + this.getClass().getName());
                setWorkDone();
            }
        }
    }

    /**
     * Abstract method for actual work implementation.
     */
    protected abstract void work();

    /**
     * Set the internal state of this work as complete.
     */
    private void setWorkDone() {
        this.workDone = Boolean.TRUE;
    }

    /**
     * Check to see if the work is complete.
     * 
     * @return True if the work is complete.
     */
    public boolean isWorkDone(){
        return workDone;
    }

    /**
     * Download a license file from the cache to the local filesystem
     * 
     * @param loader
     * @param licensePath
     */
    protected void downloadLicense(final FileLoader loader, final String licensePath) {
        String licenseFileName = (new File(
                configuration.getLicenseFileLocation())).getName();
        try {
            loader.download(licenseFileName, licensePath);
        } catch (FileNotFoundException e) {
            log.warn("No license file was found in the cache. You may see failures if you use enterprise features.");
        } catch (IOException e) {
            log.warn(
                    "Error downloading the license file from the cache. You may see errors if you use enterprise features.",
                    e);
        }
    }

}
