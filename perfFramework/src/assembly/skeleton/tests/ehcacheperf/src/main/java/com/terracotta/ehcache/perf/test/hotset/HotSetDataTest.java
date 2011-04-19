/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

import com.terracotta.ehcache.perf.HotSetConfiguration;
import com.terracotta.ehcache.perf.test.AbstractTest;

/**
 * @author abhi.sanoujam
 */
public abstract class HotSetDataTest extends AbstractTest {

  enum DistributionType {
    simple,
    simpleMoving
  }
  private Distribution distribution;

  public Distribution getDistribution() {
    if(distribution == null) {
      HotSetConfiguration hotsetConf = driver.getConfiguration().getHotSetConfiguration();
      if (hotsetConf.getDistributionType().equals(DistributionType.simpleMoving))
        distribution = SimpleMovingDistribution.getSimpleMovingDistribution(hotsetConf);
      else
        distribution = SimpleDistribution.getSimpleDistribution(hotsetConf);
    }
    return distribution;
  }

  protected int nextReadAccountNumberByDistribution() {
    return getDistribution().getNextSample();
  }

  public void setDistribution(Distribution distribution) {
    this.distribution = distribution;
  }

}