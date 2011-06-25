package com.terracotta.util;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.apache.log4j.Logger;

import gnu.trove.TDoubleArrayList;

/**
 * @author Alex Snaps
 */
public class LongStat {
  static final Logger    log = Logger.getLogger(LongStat.class);

  private final TDoubleArrayList data;
  private TDoubleArrayList snapshot;

  public LongStat(int initialSize) {
    data = new TDoubleArrayList(initialSize);
  }

  /**
   * Add value
   */
  public void add(long value) {
    if (value > Short.MAX_VALUE) {
      log.warn("stat value exceeds 32 secs, value = " + value);
    }
    synchronized (data) {
      data.add(value);
    }
  }

  /**
   * Add values from LongStat
   */
  public void add(LongStat value) {
    value.snapshot();
    synchronized (data) {
      data.add(value.getSnapshotArray());
    }
  }
  /**
   * Create a snapshot on which to draw min/max/average, etc.
   */
  public void snapshot() {
    synchronized (data) {
      snapshot = (TDoubleArrayList) data.clone();
    }
  }

  public double getMin() {
    if (snapshot == null)
      snapshot();

    double min = Double.MAX_VALUE;
    for (int i = 0; i < snapshot.size(); i++) {
      double value = snapshot.get(i);
      if (value < min) {
        min = value;
      }
    }

    if (min == Double.MAX_VALUE) return 0;

    return min;
  }

  public double getMax() {
    if (snapshot == null)
      snapshot();

    double max = Double.MIN_VALUE;
    for (int i = 0; i < snapshot.size(); i++) {
      double value = snapshot.get(i);
      if (value > max) {
        max = value;
      }
    }

    if (max == Double.MIN_VALUE) return 0;

    return max;
  }

  public double getTotal() {
    if (snapshot == null)
      snapshot();

    double sum = 0;
    for (int i = 0; i < snapshot.size(); i++) {
      double value = snapshot.get(i);
      sum += value;
    }
    return sum;
  }

  public double getAverage() {
    if (snapshot == null)
      snapshot();

    double sum = getTotal();
    return (snapshot.size() < 1 ? sum : sum / snapshot.size());
  }

  public int size(){
    if (snapshot == null)
      snapshot();

    return snapshot.size();
  }

  public double[] getSnapshotArray() {
    if (snapshot == null)
      snapshot();

    return snapshot.toNativeArray();
  }

  public void reset() {
    synchronized (data) {
      data.resetQuick();
      snapshot = null;
    }
  }

  public double getPercentile(double p){
    Percentile perc = new Percentile();
    return perc.evaluate(getSnapshotArray(), p);
  }

  @Override
  public String toString() {
    return String.format("Min: %.1f, Max: %.1f, Avg: %.5f, Median: %.1f, 95th Percentile: %.1f, 99th Percentile: %.1f", getMin(), getMax(),
                         getAverage(), getPercentile(50), getPercentile(95), getPercentile(99));
  }
}
