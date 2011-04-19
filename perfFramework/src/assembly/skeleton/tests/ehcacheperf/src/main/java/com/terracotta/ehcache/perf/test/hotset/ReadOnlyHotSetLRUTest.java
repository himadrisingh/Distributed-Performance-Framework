/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

/**
 * @author abhi.sanoujam
 */
public class ReadOnlyHotSetLRUTest extends HotSetDataTest {

  public void doTestBody() {
    // get the next account number to read.. use the distribution specified
    //perfActions.getOwner(nextReadAccountNumberByDistribution());
    if (clinic.getOwner(nextReadAccountNumberByDistribution()) == null) {
        throw new RuntimeException("Key has no value in the cache");
    }
  }

}
