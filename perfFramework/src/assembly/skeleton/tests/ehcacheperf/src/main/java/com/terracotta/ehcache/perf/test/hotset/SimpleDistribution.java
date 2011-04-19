/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

import java.util.Random;

import com.terracotta.ehcache.perf.HotSetConfiguration;
import com.terracotta.util.RangeUtil;

/**
 * @author abhi.sanoujam
 */
public class SimpleDistribution implements Distribution {

  private final int         hotPercent;
  private final RangeUtil   hotData;
  private final RangeUtil   nonHotData;
  private final Random      random = new Random(System.currentTimeMillis());
  private final boolean     debugDistributionEnabled;
  private DistributionDebug distribution;

  public static Distribution getSimpleDistribution(HotSetConfiguration config) {
    return new SimpleDistribution(config.isDebugDistributionEnabled(), config.getSimpleDistributionHotPercent(), config
        .getSimpleDistributionHotSetDataPercent(), config.getTotalDataSetSize());
  }

  public SimpleDistribution(boolean debugDistributionEnabled, int hotPercent, int hotsetDataPercent, long dataSetSize) {
    this.hotPercent = hotPercent;
    long hotSetMax = (hotsetDataPercent * dataSetSize) / 100;
    this.hotData = new RangeUtil(0, hotSetMax);
    this.nonHotData = new RangeUtil(hotSetMax, dataSetSize - 1);
    this.debugDistributionEnabled = debugDistributionEnabled;
    this.distribution = new DistributionDebug(dataSetSize);
  }

  public int getNextSample() {
    int rv;
    boolean hot;
    if ((random.nextInt(100) + 1) > hotPercent) {
      // hit a nonHot data
      int rand = random.nextInt((int) (nonHotData.getMax() - nonHotData.getMin() + 1));
      rv = (int) (nonHotData.getMin() + rand);
      hot = false;
    } else {
      // hit a hot data
      rv = random.nextInt((int) hotData.getMax() + 1);
      hot = true;
    }
    if (debugDistributionEnabled) {
      distribution.addSample(rv, hot);
    }
    return rv;
  }

  public boolean isDebugDistributionEnabled() {
    return debugDistributionEnabled;
  }

  public DistributionDebug getDistributionDebug() {
    return distribution;
  }

  public static void main(String[] args) {
    final Distribution dis = new SimpleDistribution(true, 95, 50, 100);
    dis.getDistributionDebug().setDebugPrintSleepInterval(1000);
    dis.getDistributionDebug().startDebugPrintingThread();
    for (int i = 0; i < 5; i++) {
      new Thread(new Runnable() {
        public void run() {
          for (int loop = 0; loop < 100000; loop++) {
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            dis.getNextSample();
          }
        }
      }).start();
    }
  }

}