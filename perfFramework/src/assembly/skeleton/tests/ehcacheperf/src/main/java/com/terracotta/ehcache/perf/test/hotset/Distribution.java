/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

/**
 * @author abhi.sanoujam
 */
public interface Distribution {

  public int getNextSample();

  public DistributionDebug getDistributionDebug();

  public boolean isDebugDistributionEnabled();

}