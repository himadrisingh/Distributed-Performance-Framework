package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Clinic;
import org.terracotta.util.ClusteredAtomicLong;

import com.terracotta.EhCachePerfTest;
import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;
import com.terracotta.cache.EhCacheWrapper;
import com.terracotta.util.StatReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alex Snaps
 */
public abstract class AbstractTest {

  private static final Logger log       = Logger.getLogger(AbstractTest.class);
  protected ClusteredAtomicLong  currKeyCount;

  protected int               nodeId;
  protected int               numberOfNodes;

  protected Random            random    = new Random();
  protected int               threadNum = 5;
  protected boolean           singleThreadedWarmup = false;
  protected int               maxKeyValue;
  protected Clinic            clinic;
  protected EhCachePerfTest   driver;
  protected int               writePercentage = 2;
  private final AtomicInteger writesCount = new AtomicInteger();

  public abstract void doTestBody();

  public Clinic getClinic() {
    return clinic;
  }

  public void setDriver(EhCachePerfTest driver) {
    this.driver = driver;
  }

  public void doL2WarmUp() {
    if (singleThreadedWarmup) {
      if (nodeId > 0) {
        log.info("L2 Cache Warmup skipped");
      } else {
        loadOwnerKeyRange(1, maxKeyValue, false);
      }
    } else {
      final int startId, stopId;
      if (numberOfNodes > 1){
        int amountOfEntries = maxKeyValue / numberOfNodes;
        startId = (amountOfEntries * nodeId) + 1;
        stopId = startId + amountOfEntries;
      }
      else{
        startId = 1;
        stopId = maxKeyValue;
      }
      loadOwnerKeyRange(startId, stopId, false);
    }
  }

  public void doL1WarmUp() {

    int amountOfEntries = maxKeyValue / numberOfNodes;
    final int startId = (amountOfEntries * nodeId) + 1;
    final int stopId = startId + amountOfEntries - 1;

    long start = now();

    loadOwnerKeyRange(1, startId, true);
    loadOwnerKeyRange(stopId, maxKeyValue, true);

    double time = since(start) / 1000.0;
    log.info(String.format("L1 Cache Warmup complete, %d reads, %.1f seconds, %.1f reads/sec", maxKeyValue, time,
                           maxKeyValue / time));

  }

  protected void loadOwnerKeyRange(final int start, final int end, final boolean isL1Warmup) {
    if(start == end)
      return;

    log.info("Warming cache with keys from " + start + " to " + end);

    final AtomicLong counter = new AtomicLong();
    StatReporter reporter = new StatReporter("Loading", counter);
    reporter.untilValue(end - start);
    reporter.doSummaryReport();

    final AtomicInteger readCount = new AtomicInteger(start);

    List<Thread> workers = new ArrayList<Thread>();
    for (int i = 0; i < threadNum; i++) {
      workers.add(new Thread(new Runnable() {
        public void run() {
          int key;
          while ((key = readCount.getAndIncrement()) <= end) {
            try {
              if(isL1Warmup)
                clinic.getOwner(key);
              else
                clinic.loadOwner(key);
              counter.incrementAndGet();
            } catch (Throwable t) {
              log.fatal("Error in warm up", t);
            }
          }
        }
      }, "WARM UP THREAD #" + i));
    }

    reporter.startReporting();

    for (Thread worker : workers) {
      worker.start();
    }
    for (Thread worker : workers) {
      try {
        worker.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    reporter.completeWithSummary();
  }

  public void logMisses(boolean logMisses) {
    clinic.setLogMisses(logMisses);
  }

  protected static long now() {
    return System.currentTimeMillis();
  }

  protected static long since(long time) {
    return System.currentTimeMillis() - time;
  }

  public int getNodeId() {
    return nodeId;
  }

  public void setNodeId(int nodeId) {
    this.nodeId = nodeId;
  }

  public void setClinic(final Clinic clinic) {
    this.clinic = clinic;
  }

  public void setCurrKeyCount(ClusteredAtomicLong keyCount) {
    currKeyCount = keyCount;
  }

  public void setMaxKeyValue(final int maxKeyValue) {
    this.maxKeyValue = maxKeyValue;
  }

  public void setThreadNum(final int threadNum) {
    this.threadNum = threadNum;
  }

  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes = numberOfNodes;
  }

  public void setSingleThreadedWarmup(boolean singleThreadedWarmup) {
    this.singleThreadedWarmup = singleThreadedWarmup;
  }

  public int getWritesCount(){
    return writesCount.get();
  }

  public void addNewOwners(int count){
    log.info("### New owners are being added. ###");
    for (int i = 0; i < count; i++){
      int key = (int)currKeyCount.incrementAndGet();
      clinic.loadOwner(key);
    }
    log.info("### Added new owners. Current owners count: " + currKeyCount.get() + " ###");
  }

  protected boolean doWrite() {
    boolean isWrite = random.nextInt(100) < writePercentage;
    if (isWrite)
      writesCount.incrementAndGet();
    return isWrite;
  }

  public void setWritePercentage(final int writePercentage) {
    this.writePercentage = writePercentage;
  }

  /**
   * Called by the test driver thread just before starting test (after both L1 and L2 warmup)
   */
  public void beforeTest() {
    CacheProcessor cacheProcessor = new NonStopCacheDetailsPrinter();
    clinic.processAllCaches(cacheProcessor);
  }

  /**
   * Called by each perf test thread just before starting test body
   */
  public void beforeTestForEachAppThread() {
    // override if needed
  }

  /**
   * Called by each perf test thread just after end of test body
   */
  public void afterTestForEachAppThread() {
    // override if needed
  }

  /**
   * Called by the reporting thread every reporting interval. Override to log any reporting from the test
   */
  public void doPeriodReport() {
    // override if needed
  }

  // called at end of test, after all test threads are complete, by the main thread
  public void testComplete() {
    // override if needed
  }

  private static class NonStopCacheDetailsPrinter implements CacheProcessor {

    public void processCache(CacheWrapper cacheWrapper) {
      if (cacheWrapper instanceof EhCacheWrapper) {
        EhCacheWrapper ehcacheWrapper = (EhCacheWrapper) cacheWrapper;
        Ehcache cache = ehcacheWrapper.getCache();
        TerracottaConfiguration tcConfig = cache.getCacheConfiguration().getTerracottaConfiguration();
        boolean nonstopEnabled = tcConfig != null && tcConfig.isNonstopEnabled();
        log.info("Cache [" + cache.getName() + "], nonstop enabled: " + nonstopEnabled);
        if (nonstopEnabled) {
          NonstopConfiguration nonstopConfig = tcConfig.getNonstopConfiguration();
          log.info("   " + cache.getName() + " timeoutMillis: " + nonstopConfig.getTimeoutMillis());
          log.info("   " + cache.getName() + " immediateTimeout: " + nonstopConfig.isImmediateTimeout());
          log.info("   " + cache.getName() + " timeoutBehavior: " + nonstopConfig.getTimeoutBehavior().getType());
        }
      } else {
        log.warn("NonStopCacheTest does not know how to process cache of type: "
                 + cacheWrapper.getUnderlyingCache().getClass().getName());
      }
    }
  }
}
