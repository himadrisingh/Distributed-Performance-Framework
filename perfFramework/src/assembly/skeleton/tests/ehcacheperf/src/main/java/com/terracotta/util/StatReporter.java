/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.util;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

/**
 * e.g. new StatReporter( someLong ).reportEveryMs( 1000 ).untilValue( 5000 ).startReporting();
 * 
 * @author pat
 */
public class StatReporter {
  private String           title;
  private final AtomicLong value;
  private int              reportPeriodMs  = 1000;
  private long             targetValue     = Long.MAX_VALUE;
  private long             startTime;
  private long             startValue;
  private long             lastReportTime;
  private long             lastReportValue;
  private Logger           log;
  private boolean          stop            = false;
  private Thread           reportThread;
  private boolean          doReportSummary = false;

  public StatReporter(String title, AtomicLong value) {
    this.title = title;
    this.value = value;
    this.startValue = value.get();
    this.lastReportValue = startValue;
  }

  public StatReporter reportTo(Logger logParam) {
    this.log = logParam;
    return this;
  }

  public StatReporter reportEveryMs(int millis) {
    this.reportPeriodMs = millis;
    return this;
  }

  public StatReporter untilValue(long targetValueParam) {
    if (targetValueParam < 0) { throw new IllegalArgumentException(); }
    this.targetValue = targetValueParam;
    return this;
  }

  public StatReporter doSummaryReport() {
    this.doReportSummary = true;
    return this;
  }

  public StatReporter startReporting() {
    if (reportThread != null) { throw new IllegalStateException(); }
    lastReportTime = System.currentTimeMillis();
    if (log == null) {
      log = Logger.getLogger(title);
    }
    reportThread = new Thread() {
      @Override
      public void run() {
        doReporting();
      }
    };
    startTime = System.currentTimeMillis();
    reportThread.start();
    return this;
  }

  /**
   * Perform a graceful completion by waking and joining the report thread.
   * 
   * @See completeWithSummary()
   */
  public void complete() {
    stop = true;
    try {
      reportThread.interrupt();
      reportThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Complete and produce a summary report.
   */
  public void completeWithSummary() {
    complete();
    doSummaryReport();
  }

  private void doReporting() {
    while (!finished()) {
      try {
        Thread.sleep(reportPeriodMs);
      } catch (InterruptedException e) {
        continue;
      }
      if (!finished()) {
        doReportPeriod();
      }
    }
    if (doReportSummary) {
      doReportSummary();
    }
  }

  private void doReportSummary() {
    long valueDiff = value.get() - startValue;
    log.info(String.format("%s: Total %d tx, %.1f tx/sec", title, valueDiff, 1000.0 * valueDiff / since(startTime)));
  }

  private void doReportPeriod() {
    long now = System.currentTimeMillis();
    long nowValue = value.get();
    long valueDiff = nowValue - lastReportValue;
    log.info(String.format("%s: Period %d tx, %.1f tx/sec, Complete: %d%%", title, valueDiff, 1000.0 * valueDiff
                                                                                              / since(lastReportTime),
                           (nowValue * 100) / targetValue));
    lastReportValue = nowValue;
    lastReportTime = now;
  }

  private long since(long time) {
    return System.currentTimeMillis() - time;
  }

  private boolean finished() {
    return stop || value.get() >= targetValue;
  }
}
