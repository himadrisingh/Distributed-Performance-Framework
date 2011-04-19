/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author abhi.sanoujam
 */
public class DistributionDebug {

  private final int          BUCKET_SIZE           = 5;
  private final long         totalSize;
  private final AtomicLong[] buckets               = new AtomicLong[100 / BUCKET_SIZE];
  private final AtomicLong   totalSampleCount      = new AtomicLong();
  private final AtomicLong   hotDataSampleCount    = new AtomicLong();
  private final AtomicLong   nonHotDataSampleCount = new AtomicLong();
  private volatile Thread    debugPrinterThread;
  private volatile boolean   printDebug            = true;
  private volatile int       debugPrintSleepIntervalMillis;

  public DistributionDebug(long totalSize) {
    this.totalSize = totalSize;
    for (int i = 0; i < buckets.length; i++) {
      buckets[i] = new AtomicLong();
    }
    debugPrintSleepIntervalMillis = 5000;
  }

  public int getDebugPrintSleepInterval() {
    return debugPrintSleepIntervalMillis;
  }

  public void setDebugPrintSleepInterval(int debugPrintSleepInterval) {
    this.debugPrintSleepIntervalMillis = debugPrintSleepInterval;
  }

  public void startDebugPrintingThread() {
    if (debugPrinterThread != null) { return; }
    debugPrinterThread = debugPrintingThread();
    printDebug = true;
    debugPrinterThread.start();
  }

  public void stopDebugPrintingThread() {
    this.printDebug = false;
    this.debugPrinterThread = null;
  }

  private Thread debugPrintingThread() {
    Thread rv = new Thread(new Runnable() {

      public void run() {
        while (printDebug) {
          try {
            Thread.sleep(debugPrintSleepIntervalMillis);
            System.out.printf("Debugging Hot-Set Data distribution... printed every %s seconds%n", String
                .valueOf(debugPrintSleepIntervalMillis / 1000));
            System.out.println(DistributionDebug.this.toString());
          } catch (Exception e) {
            // ignored
          }
        }
      }

    }, "DistributionDebugPrintingThread");
    rv.setDaemon(true);
    return rv;
  }

  public void addSample(long sample, boolean hot) {
    totalSampleCount.incrementAndGet();
    if (hot) {
      hotDataSampleCount.incrementAndGet();
    } else {
      nonHotDataSampleCount.incrementAndGet();
    }
    int pc = (int) Math.ceil(sample * 100 / totalSize);
    if (pc == 0) {
      buckets[0].incrementAndGet();
      return;
    }
    int bucket = (pc / BUCKET_SIZE) - 1;
    int rem = pc % BUCKET_SIZE;
    if (rem >= 1) {
      bucket++;
    }
    buckets[bucket].incrementAndGet();
  }

  public String toString() {
    long[] snapshot = new long[buckets.length];
    int[] pcSnapshot = new int[buckets.length];
    long total = totalSampleCount.get();
    long hot = hotDataSampleCount.get();
    long nonHot = nonHotDataSampleCount.get();
    for (int i = 0; i < buckets.length; i++) {
      snapshot[i] = buckets[i].get();
      pcSnapshot[i] = (int) (total == 0 ? 0 : (snapshot[i] * 100) / total);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter out = new PrintWriter(baos);
    out.println("Hot-set data Distribution Sample (broken down in buckets of " + BUCKET_SIZE
                + "% each of the total data set):");
    for (int i = 0; i < snapshot.length; i++) {
      out.printf("%6s bucket: %-50s %s/%s samples (%s%%)%n",
                 ((i * BUCKET_SIZE) + "-" + ((i * BUCKET_SIZE) + BUCKET_SIZE)), getBars(pcSnapshot[i]), snapshot[i],
                 total, pcSnapshot[i]);
    }
    out.println("Hot Data Count: " + hot + "/" + total + " (approx. " + (hot * 100 / total) + "%) Non-Hot Data Count: "
                + nonHot + "/" + total + " (approx. " + (nonHot * 100 / total) + "%) Total Count: " + total);
    out.close();
    return baos.toString();
  }

  private String getBars(int value) {
    if (value == 0) { return ""; }
    StringBuilder rv = new StringBuilder();
    for (int i = 0; i < value; i++) {
      if (i < value - 1) rv.append("-");
      else rv.append(">");
    }
    return rv.toString();
  }

}