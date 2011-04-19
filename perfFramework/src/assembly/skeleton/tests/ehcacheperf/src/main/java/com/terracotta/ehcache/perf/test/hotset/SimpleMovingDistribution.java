/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

import org.terracotta.util.ClusteredAtomicLong;

import com.terracotta.ehcache.perf.HotSetConfiguration;
import com.terracotta.util.DefaultAtomicLongImpl;
import com.terracotta.util.RangeUtil;

import java.util.Random;

public class SimpleMovingDistribution implements Distribution {

  private final int         hotPercent;
  private final Random      random = new Random(System.currentTimeMillis());
  private final boolean     debugDistributionEnabled;
  private final int         hotsetPeriodInSecs;
  private final int         hotsetDataPercent;
  private final DistributionDebug distribution;
  private final ClusteredAtomicLong        dataSetSize;

  private RangeUtil[]   hotData;
  private RangeUtil[]   nonHotData;


  public static Distribution getSimpleMovingDistribution(HotSetConfiguration config) {
    ClusteredAtomicLong dataSet = new DefaultAtomicLongImpl();
    dataSet.set(config.getTotalDataSetSize());
    return new SimpleMovingDistribution(config.isDebugDistributionEnabled(), config.getSimpleDistributionHotPercent(), config
                                        .getSimpleDistributionHotSetDataPercent(), dataSet, config.getHotsetPeriod());
  }

  public static Distribution getSimpleMovingDistribution(HotSetConfiguration config, ClusteredAtomicLong dataSetSize) {
    return new SimpleMovingDistribution(config.isDebugDistributionEnabled(), config.getSimpleDistributionHotPercent(), config
                                        .getSimpleDistributionHotSetDataPercent(), dataSetSize, config.getHotsetPeriod());
  }

  public SimpleMovingDistribution(boolean debugDistributionEnabled, int hotPercent, int hotsetDataPercent, ClusteredAtomicLong dataSetSize, int hotsetPeriod) {
    this.hotPercent = hotPercent;
    this.dataSetSize = dataSetSize;
    this.hotsetDataPercent = hotsetDataPercent;
    this.debugDistributionEnabled = debugDistributionEnabled;
    this.distribution = new DistributionDebug(dataSetSize.get());
    hotsetPeriodInSecs = hotsetPeriod;
    setHotSetRange(0);

    Thread thread = new Thread(new MoveHotSet());
    thread.start();
  }

  public void setHotSetRange(long minHotSet){
    long dataSet = dataSetSize.get();
    long hotSet = (hotsetDataPercent * dataSet) / 100;

    if ((hotSet + minHotSet >= dataSet)){
      this.hotData = new RangeUtil[]{ new RangeUtil(0, (hotSet + minHotSet) % dataSet),
                                      new RangeUtil(minHotSet, dataSet)};
      this.nonHotData = new RangeUtil[] { new RangeUtil(0, 0),
                                          new RangeUtil((hotSet + minHotSet) % dataSet, minHotSet)};
    } else {
      this.hotData = new RangeUtil[]{ new RangeUtil(0, 0),
                                      new RangeUtil(minHotSet, (hotSet + minHotSet) % dataSet)};
      this.nonHotData = new RangeUtil[] { new RangeUtil(0, minHotSet),
                                          new RangeUtil(hotSet + minHotSet, dataSet)};
    }
    /*
     * System.out.println("HOT DATA: " + this.hotData[0].getMin() + " - " + this.hotData[0].getMax() + " , " + this.hotData[1].getMin() + " - " + this.hotData[1].getMax());
     * System.out.println("NON HOT DATA: " + this.nonHotData[0].getMin() + " - " + this.nonHotData[0].getMax() + " , " + this.nonHotData[1].getMin() + " - " + this.nonHotData[1].getMax());
     */
  }

  /* getNextSample: eg
   * Hot Range: 0-10, 30-100
   * Non-hot Range: 0-0, 10-20
   * hotRange1 = 10 - 0 = 10
   * hotRange2 = 100 - 30 = 70
   * rnd = rnd.nextInt(10+70)
   * 
   * rnd = 5 (< hotRange1): sample = 0 + 5 = 0
   * rnd = 25 (> hotRange1): sample = 30 + 25 - 5 = 50
   */

  public int getNextSample() {
    int rv;
    boolean hot;
    if ((random.nextInt(100) + 1) > hotPercent) {
      // hit a nonHot data
      long nonHotDataRange1 = nonHotData[0].getMax() - nonHotData[0].getMin();
      long nonHotDataRange2 = nonHotData[1].getMax() - nonHotData[1].getMin();

      int rand = random.nextInt((int) (nonHotDataRange1 + nonHotDataRange2));
      rv = (int) ((rand > nonHotDataRange1)? nonHotData[1].getMin() + rand - nonHotDataRange1: nonHotData[0].getMin() + rand);
      hot = false;
    } else {
      // hit a hot data
      long hotDataRange1 = hotData[0].getMax() - hotData[0].getMin();
      long hotDataRange2 = hotData[1].getMax() - hotData[1].getMin();

      int rand = random.nextInt((int) (hotDataRange1 + hotDataRange2));
      rv = (int) ((rand > hotDataRange1)? hotData[1].getMin() + rand - hotDataRange1: hotData[0].getMin() + rand);
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
    ClusteredAtomicLong dataSet = new DefaultAtomicLongImpl();
    dataSet.set(1000);
    final Distribution dis = new SimpleMovingDistribution(false, 10, 10,dataSet , 1);
    dis.getDistributionDebug().setDebugPrintSleepInterval(1000);
    dis.getDistributionDebug().startDebugPrintingThread();
    for (int i = 0; i < 5; i++) {
      new Thread(new Runnable() {
        public void run() {
          for (int loop = 0; loop < 100000; loop++) {
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            dis.getNextSample();
          }
        }
      }).start();
    }
  }

  class MoveHotSet implements Runnable{
    long hotSet;
    long minHotSet = 0;

    public MoveHotSet() {
      // This should assign hotset size based on initial data set
      hotSet = (hotsetDataPercent * dataSetSize.get()) / 100;
    }

    public void run() {

      while(true){
        try {
          Thread.sleep(hotsetPeriodInSecs * 1000);
        } catch (InterruptedException e) {
          // noop
        }
        // Hotset moves w/ half overlapping set
        minHotSet += hotSet /2;
        minHotSet = (minHotSet % dataSetSize.get());
        setHotSetRange(minHotSet);
      }
    }

  }

}
