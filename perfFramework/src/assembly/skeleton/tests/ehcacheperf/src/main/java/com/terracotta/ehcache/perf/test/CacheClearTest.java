/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.Ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.coordination.Barrier;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;
import com.terracotta.cache.EhCacheWrapper;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheClearTest extends AbstractTest {

  private static final Logger LOGGER              = LoggerFactory.getLogger(CacheClearTest.class);

  private final AtomicBoolean runningCacheClear   = new AtomicBoolean();
  private volatile boolean    doingClear          = false;
  private volatile long       cacheClearStartTime;
  private final Semaphore     testRunnerSemaphore = new Semaphore(1);

  @Override
  public void doTestBody() {
    while (true) {
      try {
        testRunnerSemaphore.acquire();
        break;
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }
    try {
      if (runningCacheClear.compareAndSet(false, true)) {
        Barrier barrier = driver.getBarrier();
        int index = barrier.await();
        cacheClearStartTime = System.nanoTime();
        if (index == 0) {
          LOGGER.info("Clearing all caches");
          this.doingClear = true;
          clinic.processAllCaches(new CacheClearProcessor());
        } else {
          LOGGER.info("Waiting for other node to clear all caches");
        }
        barrier.await();
        LOGGER.info("Clearing all caches took: " + getTimeElapsedDesc(cacheClearStartTime));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      driver.completeTest();
      testRunnerSemaphore.release();
    }
  }

  @Override
  public void doPeriodReport() {
    LOGGER.info("This node " + (doingClear ? "clearing cache" : "waiting for other node to clear cache")
                + ". Time elapsed since clear started: " + getTimeElapsedDesc(cacheClearStartTime));
  }

  private static String getTimeElapsedDesc(long sinceNanos) {
    long elapsedMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - sinceNanos, TimeUnit.NANOSECONDS);
    return (elapsedMillis / 1000) + "." + (elapsedMillis % 1000) + " secs";
  }

  public static class CacheClearProcessor implements CacheProcessor {

    public void processCache(CacheWrapper cacheWrapper) {
      if (cacheWrapper instanceof EhCacheWrapper) {
        Ehcache cache = ((EhCacheWrapper) cacheWrapper).getCache();
        LOGGER.info("Clearing cache: " + cache.getName());
        long start = System.nanoTime();
        cache.removeAll();
        LOGGER.info("Clearing cache [" + cache.getName() + "] took " + CacheClearTest.getTimeElapsedDesc(start));
      }
    }

  }
}
