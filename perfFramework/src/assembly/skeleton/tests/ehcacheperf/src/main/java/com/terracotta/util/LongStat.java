package com.terracotta.util;

import org.apache.log4j.Logger;

import gnu.trove.TLongArrayList;

/**
 * @author Alex Snaps
 */
public class LongStat {
  static final Logger    log = Logger.getLogger(LongStat.class);

  private final TLongArrayList data;
  private TLongArrayList snapshot;

  public LongStat(int initialSize) {
    data = new TLongArrayList(initialSize);
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
   * Create a snapshot on which to draw min/max/average, etc.
   */
  public void snapshot() {
    synchronized (data) {
      snapshot = (TLongArrayList) data.clone();
    }
  }

  public long getMin() {
    long min = Long.MAX_VALUE;
    for (int i = 0; i < snapshot.size(); i++) {
      long value = snapshot.get(i);
      if (value < min) {
        min = value;
      }
    }

    if (min == Long.MAX_VALUE) return 0;

    return min;
  }

  public long getMax() {
    long max = Long.MIN_VALUE;
    for (int i = 0; i < snapshot.size(); i++) {
      long value = snapshot.get(i);
      if (value > max) {
        max = value;
      }
    }

    if (max == Long.MIN_VALUE) return 0;

    return max;
  }

  public long getTotal() {
    long sum = 0;
    for (int i = 0; i < snapshot.size(); i++) {
      long value = snapshot.get(i);
      sum += value;
    }
    return sum;
  }

  public double getAverage() {
    long sum = 0;
    for (int i = 0; i < snapshot.size(); i++) {
      long value = snapshot.get(i);
      sum += value;
    }
    return (snapshot.size() < 1 ? sum : sum / (double) snapshot.size());
  }

  public int size(){
    return snapshot.size();
  }

  public long[] getSnapshotArray() {

    return snapshot.toNativeArray();
  }

  public void reset() {
    synchronized (data) {
      data.resetQuick();
      snapshot = null;
    }
  }

  @Override
  public String toString() {
    snapshot();
    return String.format("min: %d, max: %d, average: %d", getMin(), getMax(), getAverage());
  }
}
