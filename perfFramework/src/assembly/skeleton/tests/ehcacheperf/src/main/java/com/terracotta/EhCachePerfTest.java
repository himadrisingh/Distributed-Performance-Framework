package com.terracotta;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.apache.log4j.Logger;
import org.springframework.cache.ehcache.TcEhCacheManagerFactoryBean;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;
import org.terracotta.coordination.Barrier;
import org.terracotta.util.ClusteredAtomicLong;

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

  private static final long         INITIAL_VALUE = -1;
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
  private volatile int              testCountAtLastReport;

  private final LongStat            latencyStats  = new LongStat(1024 * 1024);
  private long                      cumulativeLatencyTotal;
  private long                      cumulativeLatencyMin;
  private long                      cumulativeLatencyMax;
  private long                      bulkLoadCompleteTime;

  private volatile ClusteredAtomicLong clusterWarmup;
  private volatile ClusteredAtomicLong clusterTestCount;
  private volatile ClusteredAtomicLong clusterLatency;

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

      // Counter for increasing keys over time
      ClusteredAtomicLong atomicLong = toolkit.getAtomicLong("ehcacheperf-currKeyCount");
      atomicLong.set(configuration.getElementNum());
      test.setCurrKeyCount(atomicLong);
    } else {
      this.barrier = new DefaultBarrierImpl(configuration.getNodesNum());
      clusterTestCount = new DefaultAtomicLongImpl();
      clusterLatency = new DefaultAtomicLongImpl();
      clusterWarmup = new DefaultAtomicLongImpl();
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
      try {
        log.info("Running ehcacheperf search for 1 min alone.");
        Thread.sleep(60000);
      } catch (InterruptedException e) {
        //
      }
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
            } catch (RuntimeException e) {
              log.error("error in test", e);
              testHasErrors = true;
              if (e instanceof NonStopCacheException){
                nonstopCacheExceptionCount.incrementAndGet();
              }
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

    if (period) {
      log.info("---------------------------------------------");
      if (configuration.getTestDuration() < 0)
        log.info("Test running in LRT mode.");
      else
        log.info("Remaining Time: " + Util.formatTimeInSecondsToWords((estimatedTestEndTime - now) / 1000));
    }

    // Node Period TPS
    int currentCount = nodeTestCount.get();
    long periodTestCount = currentCount - testCountAtLastReport;
    if (period) {
      log.info(String.format("Node: Period iterations/sec =  %.1f, completed = %d ",
                             1000.0 * periodTestCount / (now - lastReportTime),
                             periodTestCount));
      lastReportTime = now;
    }
    // Node Period latency
    testCountAtLastReport = currentCount;
    latencyStats.snapshot();
    double periodAvg = latencyStats.getAverage();
    long periodMax = latencyStats.getMax();
    long periodMin = latencyStats.getMin();

    if (period) {
      log.info("Node: Period latency: "
               + String.format("min: %d, max: %d, average: %.5f", periodMin, periodMax, periodAvg));
    }

    int writes = test.getWritesCount();
    if (writes > 0)
      log.info(String.format("Node: Cumulative iterations/sec =  %.1f, completed = %d , writes = %d (%.1f %%)", 1000.0 * currentCount / (now - testStartTime),
                             currentCount, writes, writes * 100.0 /currentCount));
    else
      log.info(String.format("Node: Cumulative iterations/sec =  %.1f, completed = %d", 1000.0 * currentCount / (now - testStartTime),
                             currentCount));


    this.cumulativeLatencyTotal += latencyStats.getTotal();
    if (periodMin < cumulativeLatencyMin || periodMin == INITIAL_VALUE) {
      this.cumulativeLatencyMin = periodMin;
    }

    if (periodMax > cumulativeLatencyMax || periodMax == INITIAL_VALUE) {
      this.cumulativeLatencyMax = periodMax;
    }

    long cumulativeMin = this.cumulativeLatencyMin;
    long cumulativeMax = this.cumulativeLatencyMax;
    double cumulativeAvg = (double) this.cumulativeLatencyTotal / currentCount;

    // Log latency stats and reset on each report.
    log.info("Node: Cumulative latency: "
             + String.format("min: %d, max: %d, average: %.5f", cumulativeMin, cumulativeMax, cumulativeAvg));
    latencyStats.reset();
    if (period) {
      test.doPeriodReport();
    }

    if (testHasErrors) {
      log.error("Node: Test has errors. NonstopCacheException: " + nonstopCacheExceptionCount.get());
    }
    lastReportTime = now;
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
    clusterTestCount.addAndGet(nodeTestCount.intValue());

    if (getConfiguration().isStandalone())
      clusterLatency.addAndGet(cumulativeLatencyTotal * 100 /testCountAtLastReport);    // * 100 to get better average as it will convert to Long

    await();
    long totalNode = nodeTestCount.get();
    long totalCluster = clusterTestCount.get();
    log.info("------- FINAL REPORT -------- ");
    long testDuration = (actualTestEndTime - testStartTime) / 1000;
    log.info(String.format("Node TPS: %.1f", (double) totalNode  / testDuration));
    log.info(String.format("Cluster TPS: %.1f", (double) totalCluster / testDuration));

    if (getConfiguration().isStandalone()){
      log.info(String.format("Cluster Avg Latency: %.1f", (double) clusterLatency.get() / (configuration.getNodesNum() * 100 )));
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

