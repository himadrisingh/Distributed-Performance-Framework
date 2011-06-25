package com.terracotta;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.apache.log4j.Logger;
import org.springframework.cache.ehcache.TcEhCacheManagerFactoryBean;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;
import org.terracotta.coordination.Barrier;
import org.terracotta.util.ClusteredAtomicLong;

import com.terracotta.cache.CacheStatsProcessor;
import com.terracotta.ehcache.perf.Configuration;
import com.terracotta.ehcache.perf.HotSetConfiguration;
import com.terracotta.ehcache.perf.test.AbstractTest;
import com.terracotta.ehcache.search.SearchExecutor;
import com.terracotta.util.DefaultAtomicLongImpl;
import com.terracotta.util.DefaultBarrierImpl;
import com.terracotta.util.LongStat;
import com.terracotta.util.SpringFactory;
import com.terracotta.util.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicInteger;

public class EhCachePerfTest {

  // Needed for Destructive tests L1 restarts. Restarted L1s should skip barriers all the time.
  private static final boolean      skipBarrier = Boolean.parseBoolean(System.getProperty("skip.barrier", "false"));

  private static final Logger       log           = Logger.getLogger(EhCachePerfTest.class);
  private static final String       BARRIER_ROOT  = "BARRIER_ROOT";

  private final Configuration       configuration;
  private final int                 nodeId;
  private final Barrier             barrier;
  private final AbstractTest        test;
  private List<Thread>              threads;
  private long                      testStartTime;
  private long                      lastReportTime;
  private long                      estimatedTestEndTime;
  private long                      actualTestEndTime;

  private final AtomicInteger       nonstopCacheExceptionCount = new AtomicInteger();
  private volatile boolean          testHasErrors;
  private volatile boolean          testComplete;
  private Thread                    reporterThread;
  private final AtomicInteger       nodeTestCount = new AtomicInteger();
  private volatile int              testCountAtLastReport, readCountAtLastReport, writeCountAtLastReport;

  private final LongStat            latencyStats  = new LongStat(1024 * 1024);
  private final LongStat            cumulativeLatencyStats  = new LongStat(1024 * 1024);

  private long                      bulkLoadCompleteTime;

  private volatile ClusteredAtomicLong clusterWarmup, clusterTestCount, clusterLatency;
  private volatile ClusteredAtomicLong clusterReads, clusterWrites, clusterCacheWarmup;

  private final CacheStatsProcessor processor = CacheStatsProcessor.getInstance();

  private final TcEhCacheManagerFactoryBean ehcacheBean;

  public EhCachePerfTest(final Configuration configuration) {
    SpringFactory.getApplicationContext(configuration);
    ehcacheBean = SpringFactory.getControllerBean(configuration, TcEhCacheManagerFactoryBean.class);
    if (ehcacheBean == null) {
      log.warn("Cant find TcEhCacheManagerFactoryBean. Load incoherently WON'T work.");
    }

    this.test = configuration.getTestCase().getTest();
    if (test == null) throw new RuntimeException("Test case is null.");
    log.info("XXXXX running test: " + this.test.getClass().getSimpleName());
    test.setDriver(this);
    this.configuration = configuration;
    if (configuration.isStandalone()) {
      ClusteringToolkit toolkit = new TerracottaClient(configuration.getExpressTerracottaUrl()).getToolkit();
      this.barrier = toolkit.getBarrier(BARRIER_ROOT, configuration.getNodesNum());
      clusterTestCount = toolkit.getAtomicLong("ehcacheperf-clusterTestCount");
      clusterLatency = toolkit.getAtomicLong("ehcacheperf-clusterLatency");
      clusterWarmup = toolkit.getAtomicLong("ehcacheperf-warmup");
      clusterReads = toolkit.getAtomicLong("ehcacheperf-clusterReads");
      clusterWrites = toolkit.getAtomicLong("ehcacheperf-clusterWrites");
      clusterCacheWarmup = toolkit.getAtomicLong("ehcacheperf-clusterCacheWarmup");

      // Counter for increasing keys over time
      ClusteredAtomicLong atomicLong = toolkit.getAtomicLong("ehcacheperf-currKeyCount");
      atomicLong.set(configuration.getElementNum());
      test.setCurrKeyCount(atomicLong);
    } else {
      this.barrier = new DefaultBarrierImpl(configuration.getNodesNum());
      clusterTestCount = new DefaultAtomicLongImpl();
      clusterLatency = new DefaultAtomicLongImpl();
      clusterWarmup = new DefaultAtomicLongImpl();
      clusterReads = new DefaultAtomicLongImpl();
      clusterWrites = new DefaultAtomicLongImpl();
      clusterCacheWarmup = new DefaultAtomicLongImpl();
    }
    this.nodeId = await();
    test.setNodeId(nodeId);
    test.setNumberOfNodes(configuration.getNodesNum());
  }

  public Barrier getBarrier() {
    return barrier;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  private void setBulkLoad(boolean bulkLoad){
    if(configuration.getCacheType().equals("ehcache") && ehcacheBean != null)
      ehcacheBean.setBulkLoad(bulkLoad);
  }

  private void waitUntilBulkLoadComplete(){
    if(configuration.getCacheType().equals("ehcache") && ehcacheBean != null){
      ehcacheBean.setBulkLoad(false);
      log.info("Waiting for all nodes to be coherent.");
      ehcacheBean.waitUntilBulkLoadComplete();
    }
  }

  private void runTest() {
    log.info("Welcome on node #" + nodeId + ", total nodes: " + configuration.getNodesNum());
    log.info("Starting warmup phase");
    try {
      log.info("Starting L2 Warmup phase.");
      log.info("Loading cache: bulk load enabled : " + configuration.isBulkLoadEnabled());
      setBulkLoad(configuration.isBulkLoadEnabled());

      long start = now();
      test.doL2WarmUp();
      long end = now();

      long count = configuration.getElementNum() / configuration.getNodesNum();
      long time = (end - start) / 1000;
      time = (time == 0) ? 1 : time;
      log.info(String.format("L2 Cache Warmup complete, %d reads, %d seconds, %.1f reads/sec", count, time, count * 1.0
                             / time));
      clusterWarmup.addAndGet(count/time);

      test.processCacheStats();
      int warmup = processor.getWrite();
      LongStat warmupStats = processor.getWriteStat();
      log.info(String.format("Cache Warmup: %d puts, %d seconds, %.1f puts/sec", warmup, time, warmup * 1.0 / time));
      log.info("Cache Warmup Latency: " + warmupStats.toString());
      clusterCacheWarmup.addAndGet(warmup / time);
      test.resetCacheStats();

      log.info("Waiting for all nodes to complete L2 warmup.");
      await();
      start = now();
      waitUntilBulkLoadComplete();
      bulkLoadCompleteTime = now() - start;
      await();
      if (configuration.isL1Enabled()) {
        log.info("Starting L1 Warmup phase.");
        setBulkLoad(configuration.isBulkLoadEnabled());
        test.doL1WarmUp();
      }
    } catch (Exception e) {
      log.fatal("Error during warm up phase!", e);
      System.exit(-1);
    }
    log.info("Warmup phase done... Waiting for the other nodes to be ready");
    setBulkLoad(false);
    await();
    log.info("Starting the actual test phase now!");
    if (configuration.isLogMisses()) {
      test.logMisses(true);
    }
    test.beforeTest();

    if (configuration.isSearchEnabled()){
      SearchExecutor exec = new SearchExecutor(configuration, ehcacheBean);
      exec.run();
    }

    testStartTime = now();
    lastReportTime = now();
    estimatedTestEndTime = testStartTime + (configuration.getTestDuration() * 1000);

    this.threads = new ArrayList<Thread>(configuration.getThreadNum());
    for (int i = 0; i < configuration.getThreadNum(); i++) {
      threads.add(new Thread("PerfAppThread-" + i) {
        @Override
        public void run() {
          test.beforeTestForEachAppThread();
          while (isTestRunning()) {
            long start = now();
            try {
              test.doTestBody();
            }
            catch (NonStopCacheException ne){
              nonstopCacheExceptionCount.incrementAndGet();
            }
            catch (RuntimeException e) {
              log.error("error in test", e);
              testHasErrors = true;
            }
            iterationComplete(now() - start);
          }
          test.afterTestForEachAppThread();
        }
      });
    }

    if(configuration.isAddOwnersEnabled()){
      log.info("Starting add-owners thread. " + getConfiguration().getAddOwnersCount()
               + " new owners will be added every "
               + getConfiguration().getAddOwnersPeriodInSecs()
               + " secs.");
      new Thread() {
        @Override
        public void run() {
          while (isTestRunning()) {
            try {
              sleep(getConfiguration().getAddOwnersPeriodInSecs() * 1000);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            test.addNewOwners(getConfiguration().getAddOwnersCount());
          }
        }
      }.start();
    }else
      log.info("Not adding new owners.....");

    for (Thread thread : threads) {
      thread.start();
    }
    startReporterThread();
    waitForTestThreads();
    waitForReporterThread();
    doFinalReport();
  }

  private void iterationComplete(final long time) {
    nodeTestCount.incrementAndGet();
    latencyStats.add(time);
  }

  private boolean isTestRunning(){
    return (!testComplete && now() < estimatedTestEndTime) || configuration.getTestDuration() < 0;
  }

  private void waitForTestThreads() {
    // wait for all threads to complete
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    test.testComplete();
    this.actualTestEndTime = now();
  }

  public void completeTest() {
    this.testComplete = true;
  }

  private void waitForReporterThread() {
    // wait for the reporter thread to exit and then do the final report
    try {
      reporterThread.interrupt();
      reporterThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void startReporterThread() {
    this.reporterThread = new Thread() {
      @Override
      public void run() {
        while (isTestRunning()) {
          try {
            sleep(configuration.getReportInterval() * 1000);
          } catch (InterruptedException e) {
            // ignored
          }
          doPeriodReport();
        }
      }
    };
    reporterThread.start();
  }

  private void doPeriodReport() {
    doPeriodReport(true);
  }

  private void doPeriodReport(final boolean period) {
    if (period && !isTestRunning()) return;

    long now = now();
    int currentCount = nodeTestCount.get();
    latencyStats.snapshot();
    this.cumulativeLatencyStats.add(latencyStats);

    if (period) {
      log.info("");
      log.info("------------------ Test Stats -----------------------");
      if (configuration.getTestDuration() < 0) {
        log.info("Test running for " + Util.formatTimeInSecondsToWords((now - testStartTime) / 1000));
      } else {
        log.info("Remaining Time: " + Util.formatTimeInSecondsToWords((estimatedTestEndTime - now) / 1000));
      }

      /* *******************************************************
       * Node Periodic Stats
       * *******************************************************/
      int periodTestCount = currentCount - testCountAtLastReport;
      log.info(String.format("Node: Period iterations/sec =  %.1f, completed = %d ", 1000.0 * periodTestCount
                             / (now - lastReportTime),
                             periodTestCount));

      log.info("Node: Period latency: " + latencyStats);

      testCountAtLastReport = currentCount;
    }

    /* *******************************************************
     * Node Cumulative Stats
     * *******************************************************/
    int writes = test.getWritesCount();
    if (writes > 0) {
      log.info(String.format("Node: Cumulative iterations/sec =  %.1f, completed = %d , writes = %d (%.1f %%)",
                             1000.0 * currentCount / (now - testStartTime), currentCount, writes, writes * 100.0
                             / currentCount));
    } else {
      log.info(String.format("Node: Cumulative iterations/sec =  %.1f, completed = %d", 1000.0 * currentCount
                             / (now - testStartTime),
                             currentCount));
    }
    log.info("Node: Cumulative latency: " + cumulativeLatencyStats);

    latencyStats.reset();
    printCacheStats(period);

    if (testHasErrors) {
      log.error("Node: Test has errors. NonstopCacheException: " + nonstopCacheExceptionCount.get());
    }
    lastReportTime = now;
  }

  private void printCacheStats(final boolean period){
    long now = now();
    test.processCacheStats();

    // Cache detailed stats
    LongStat readStat = processor.getReadStat();
    LongStat writeStat = processor.getWriteStat();
    readStat.snapshot();
    writeStat.snapshot();

    int readCount = processor.getRead();
    int writeCount = processor.getWrite();
    int total = readCount + writeCount;

    if (period){
      /* *******************************************************
       * Node Periodic Cache Stats
       * *******************************************************/
      log.info("------------------ Cache Stats -----------------------");

      int periodReadCount = readCount - readCountAtLastReport;
      int periodWriteCount = writeCount - writeCountAtLastReport;
      int periodTotalCount = (total) - (readCountAtLastReport + writeCountAtLastReport);

      log.info(String.format("Cache: Period Read iterations/sec =  %.1f, completed = %d ", 1000.0 * periodReadCount
                             / (now - lastReportTime),
                             periodReadCount));
      log.info(String.format("Cache: Period Write iterations/sec =  %.1f, completed = %d ", 1000.0 * periodWriteCount
                             / (now - lastReportTime),
                             periodWriteCount));
      log.info(String.format("Cache: Period iterations/sec =  %.1f, completed = %d ", 1000.0 * periodTotalCount
                             / (now - lastReportTime),
                             periodTotalCount));

      readCountAtLastReport = readCount;
      writeCountAtLastReport = writeCount;
    }

    /* *******************************************************
     * Cumulative Cache stats
     * *******************************************************/
    log.info(String.format("Cache: Cumulative Read iterations/sec =  %.1f, completed = %d", 1000.0 * readCount / (now - testStartTime),
                           readCount));
    log.info(String.format("Cache: Cumulative Write iterations/sec =  %.1f, completed = %d", 1000.0 * writeCount / (now - testStartTime),
                           writeCount));
    log.info(String.format("Cache: Cumulative Total iterations/sec =  %.1f, completed = %d", 1000.0 * total / (now - testStartTime),
                           total));
    log.info("Cache: Cumulative Read latency: " + readStat);
    log.info("Cache: Cumulative Write latency: " + writeStat);

    processor.reset();
  }

  public static void main(String[] args) throws Exception {
    log.info("EhCache Performance Test Application");
    if (args.length != 1) {
      log.fatal("You need to provide a valid properties file as argument");
      System.exit(-1);
    }
    String propertiesLine = args[0];
    log.info("Property file location: " + propertiesLine);
    Properties props = loadProperties(propertiesLine);
    log.info("Properties: " + props);
    Configuration conf = new Configuration(props);

    // changes for hot data set
    conf.setHotSetConfiguration(HotSetConfiguration.getHotSetConfig(conf, props));

    log.info("Running test case " + conf.getTestCase());
    log.info("\n" + conf);
    new EhCachePerfTest(conf).runTest();
    log.info("Done with test case " + conf.getTestCase() + ". Hoping to see you again soon...");
    System.exit(0);
  }

  public void doFinalReport() {
    await();
    doPeriodReport(false);
    test.processCacheStats();
    clusterTestCount.addAndGet(nodeTestCount.intValue());
    clusterReads.addAndGet(processor.getRead());
    clusterWrites.addAndGet(processor.getWrite());

    // * 100 to get better average as it will convert to Long
    if (getConfiguration().isStandalone()) {
      clusterLatency.addAndGet((long) cumulativeLatencyStats.getTotal() * 100 / testCountAtLastReport);
    }
    await();
    long totalNode = nodeTestCount.get();
    long totalCluster = clusterTestCount.get();
    long totalRead = clusterReads.get();
    long totalWrite = clusterWrites.get();
    log.info("------- FINAL REPORT -------- ");
    long testDuration = (actualTestEndTime - testStartTime) / 1000;
    log.info(String.format("Node TPS: %.1f", (double) totalNode  / testDuration));
    log.info(String.format("Cluster TPS: %.1f", (double) totalCluster / testDuration));
    log.info("------- Cache Stats -------- ");
    log.info(String.format("Cluster Cache Read TPS: %.1f", (double) totalRead / testDuration));
    log.info(String.format("Cluster Cache Write TPS: %.1f", (double) totalWrite / testDuration));
    log.info(String.format("Cluster Cache Total TPS: %.1f", (double) (totalRead + totalWrite) / testDuration));

    if (getConfiguration().isStandalone()){
      log.info(String.format("Cluster Avg Latency: %.1f", (double) clusterLatency.get() / (configuration.getNodesNum() * 100 )));
      log.info(String.format("Warmup Cache TPS: %d", clusterCacheWarmup.get()));
      log.info(String.format("Warmup Cluster TPS: %d , Time taken for clusterCoherent: %d", clusterWarmup.get(), bulkLoadCompleteTime));
    }
    int totalWrites = test.getWritesCount();
    if (totalWrites > 0){
      log.info(String.format("Node Total Write operations: %d (%.1f %%)", totalWrites, (totalWrites * 100.0/totalNode)));
    }

    int exceptions = nonstopCacheExceptionCount.get();
    if (exceptions > 0){
      log.info(String.format("Node NonstopCache Exception Count: %d (%.1f %%)", exceptions , (exceptions * 100.0/totalNode)));
    }
  }

  private static Properties loadProperties(final String location) {
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return props;
  }

  protected static long now() {
    return System.currentTimeMillis();
  }

  private int await(){
    if (skipBarrier){
      log.warn("Skipping barriers.....");
      return configuration.getNodesNum();
    }

    int parties = -1;
    try {
      parties = barrier.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    }
    return parties;
  }

  public long getTestElapsedTimeSeconds() {
    return (now() - testStartTime) / 1000;
  }
}

