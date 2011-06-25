package com.terracotta.ehcache.perf.test;

import org.apache.log4j.Logger;

import com.terracotta.util.Util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheGetSizeTest extends AbstractTest {

  private static final Logger log                 = Logger.getLogger(CacheGetSizeTest.class);

  private final AtomicInteger numThreadsAtomicInt = new AtomicInteger();
  private long                startTime           = 0;
  private int                 numThreads;
  private final AtomicBoolean testComplete        = new AtomicBoolean(false);

  @Override
  public void doTestBody() {
    Map<String, Integer> map = clinic.cacheGetSizes();
    for (Map.Entry<String, Integer> e : map.entrySet()) {
      log.info("CachegetSize:" + e.getKey() + " has " + e.getValue());
    }
  }

  @Override
  public void beforeTestForEachAppThread() {
    if (numThreadsAtomicInt.incrementAndGet() == 1) {
      startTime = now();
      log.info("Starting CacheGetSize test");
    }
  }

  @Override
  public void afterTestForEachAppThread() {
    int n = numThreadsAtomicInt.get();
    if (testComplete.compareAndSet(false, true)) {
      this.numThreads = n;
    }
    if (numThreadsAtomicInt.decrementAndGet() == 0) {
      // test complete
      double seconds = (now() - startTime) / 1000d;
      log.info(String.format("Node-%d: Local getSize by each test-thread complete. took %.1f secs. (%s) %d threads",
                             this.nodeId, seconds, Util.formatTimeInSecondsToWords((long) seconds), this.numThreads));
    }
  }

  @Override
  public void testComplete() {
    log.info("Local getSize done. All threads completed loading.");
    double seconds = (now() - startTime) / 1000d;
    log.info(String.format("Node-%d: getSize test complete. Took %.1f secs. (%s) %d threads", this.nodeId, seconds,
                           Util.formatTimeInSecondsToWords((long) seconds), this.numThreads));
  }

}
