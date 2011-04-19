package com.terracotta.ehcache.perf.test;

/**
 * @author Alex Snaps
 */
public class PartitionedReadOnlyTest extends PartitionedTest {

  public void doTestBody() {
    int key = partitionStart + this.random.nextInt((partitionEnd - partitionStart));
    // log.info("Key = " + key + " partitionStart# " + partitionStart + " partitionEnd# " + partitionEnd );
    if (clinic.getOwner(key + 1) == null) { throw new RuntimeException("Key '" + key + "' has no value in the cache"); }
  }
}
