package com.terracotta.ehcache.perf.test;

import com.terracotta.util.SpringFactory;

/**
 * @author Alex Snaps
 */
public enum TestCase {
  dummyTest,
  readOnlyTest,
  readWriteTest,
  readWriteBehindTest,
  nonStopCacheReadWriteTest,
  partitionedReadWriteTest,
  nonStopCachePartitionedReadWriteTest,
  partitionedReadOnlyTest,
  bulkLoadTest,
  jtaTest,
  nonStopCacheTest,
  evictionTest,
  hotSetReadOnlyTest,
  hotSetReadWriteTest,
  readWriteMovingHotSetTest,
  movingKeySetTest,
  cacheGetSizeTest,
  cacheClearTest;

  public AbstractTest getTest() {
    return SpringFactory.getBean(name());
  }
}
