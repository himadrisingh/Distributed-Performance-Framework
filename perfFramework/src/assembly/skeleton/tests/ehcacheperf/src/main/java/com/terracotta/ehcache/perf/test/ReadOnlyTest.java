package com.terracotta.ehcache.perf.test;

/**
 * @author Alex Snaps
 */
public class ReadOnlyTest extends AbstractTest {

  public void doTestBody() {
    int key = this.random.nextInt(maxKeyValue);
    if (clinic.getOwner(key + 1) == null) { throw new RuntimeException("Key '" + key + "' has no value in the cache"); }
  }

}
