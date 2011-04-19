package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;

import org.apache.log4j.Logger;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;
import com.terracotta.cache.EhCacheWrapper;
import com.terracotta.ehcache.perf.Configuration;
import com.terracotta.util.Util;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BulkLoadTest extends AbstractTest implements CacheProcessor {

  private static final Logger log                        = Logger.getLogger(BulkLoadTest.class);

  private static final String BULK_LOAD_TEST_VERIFY_MODE = "bulkLoadTest.verify";

  private final AtomicInteger loadOwnerRange             = new AtomicInteger();
  private int                 startId;
  private int                 endId;
  private final AtomicInteger numThreadsAtomicInt        = new AtomicInteger();
  private long                startTime                  = 0;
  private int                 numThreads;
  private int                 total;
  private final AtomicBoolean testComplete               = new AtomicBoolean(false);

  @Override
  public void beforeTest() {
    super.beforeTest();
    Configuration config = driver.getConfiguration();
    String testModeStr = config.getProperties().getProperty(BULK_LOAD_TEST_VERIFY_MODE);
    if (testModeStr == null || "true".equalsIgnoreCase(testModeStr)) {
      log.info("Running bulk load in verify mode");
      clinic.processAllCaches(new NoGetOpCacheConverter());
    } else {
      log.info("Not running bulk load verify mode");
    }
  }

  @Override
  public void doL1WarmUp() {
    // no-op
  }

  @Override
  public void doL2WarmUp() {
    int amountOfEntries = maxKeyValue / numberOfNodes;
    startId = (amountOfEntries * nodeId) + 1;
    endId = startId + amountOfEntries - 1;
    total = endId - startId + 1;
    loadOwnerRange.set(startId);
    log.info("Node-" + nodeId+ " loading owner id range: [" + loadOwnerRange.get() + ", " + endId + "]");
  }

  @Override
  public void doTestBody() {
    int currentOwnerId = loadOwnerRange.getAndIncrement();
    if (currentOwnerId <= endId) {
      clinic.loadOwner(currentOwnerId);
    } else {
      driver.completeTest();
    }
  }

  @Override
  public void beforeTestForEachAppThread() {
    if (numThreadsAtomicInt.incrementAndGet() == 1) {
      startTime = now();
      log.info("Starting bulk load test");
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
      log
          .info(String
              .format(
                      "Node-%d: Local load by each test-thread complete. load TPS: %.1f, Loaded %d in %.1f secs. (%s) %d threads",
                      this.nodeId, total / seconds, total, seconds, Util.formatTimeInSecondsToWords((long) seconds),
                      this.numThreads));
    }
  }

  @Override
  public void doPeriodReport() {
    log.info("Running test since: " + Util.formatTimeInSecondsToWords((now() - startTime) / 1000));
    int loaded = loadOwnerRange.get() - startId + 1;
    double pc = ((double) loaded * 100) / total;
    log.info("Complete: " + String.format("%3.2f", pc) + "%, loaded: " + loaded + "/" + total);
  }

  @Override
  public void testComplete() {
    log.info("Local puts done. All threads completed loading.");
    clinic.processAllCaches(this);
    double seconds = (now() - startTime) / 1000d;
    log.info(String.format("Node-%d: Bulk Load test complete. load TPS: %.1f, Loaded %d in %.1f secs. (%s) %d threads",
                           this.nodeId, total / seconds, total, seconds, Util
                               .formatTimeInSecondsToWords((long) seconds), this.numThreads));
  }

  public void processCache(CacheWrapper cacheWrapper) {
    if (cacheWrapper instanceof EhCacheWrapper) {
      Ehcache cache = ((EhCacheWrapper) cacheWrapper).getCache();
      log.info("Calling Ehcache.setCoherent(true) for the cache [" + cache.getName() + "]...");
      // set cache to incoherent first
      // this is because, if test is running in:
      // - incoherent mode: this is no-op
      // - coherent mode: calling cache.setCoherent(true) after this will make the thread wait till all pending txns are
      // committed, so that TPS comparison between coherent mode and incoherent mode is fair
      cache.setNodeCoherent(false);
      long now = now();
      cache.setNodeCoherent(true);
      log.info("... setCoherent(true) for [" + cache.getName() + "] took: " + (now() - now) + " msecs");
    } else {
      log.warn("BulkLoadTest does not know how to process cache of type: "
               + cacheWrapper.getUnderlyingCache().getClass().getName());
    }
  }

  private static class NoGetOpCacheConverter implements CacheProcessor {

    public void processCache(CacheWrapper cacheWrapper) {
      if (cacheWrapper instanceof EhCacheWrapper) {
        EhCacheWrapper ehcacheWrapper = (EhCacheWrapper) cacheWrapper;
        Ehcache cache = ehcacheWrapper.getCache();
        log.info("Throwing exception on get operations for cache: " + cache.getName());
        Ehcache decoratedCache = new GetNotSupportedCache(cache);
        ehcacheWrapper.replaceCacheWithDecoratedCache(decoratedCache);
      } else {
        log.warn("NoGetOpCacheConverter does not know how to process cache of type: "
                 + cacheWrapper.getUnderlyingCache().getClass().getName());
      }
    }
  }

  private static class GetNotSupportedCache extends EhcacheDecoratorAdapter {

    public GetNotSupportedCache(Ehcache underlyingCache) {
      super(underlyingCache);
    }

    @Override
    public Element get(Object key) throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public List getKeys() throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

    @Override
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
      throw new RuntimeException("This operation should not be invoked");
    }

  }
}
