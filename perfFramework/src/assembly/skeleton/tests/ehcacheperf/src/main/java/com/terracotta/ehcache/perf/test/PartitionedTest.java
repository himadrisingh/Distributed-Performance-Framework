package com.terracotta.ehcache.perf.test;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Clinic;

/**
 * @author Alex Snaps
 */
public abstract class PartitionedTest extends AbstractTest {

  private static final Logger log = Logger.getLogger(PartitionedTest.class);
  protected int               partitionStart, partitionEnd;

  @Override
  public abstract void doTestBody();

  @Override
  public void doL2WarmUp(){
    int amountOfEntries = maxKeyValue / numberOfNodes;
    final int startId = (amountOfEntries * nodeId) + 1;
    final int stopId = startId + amountOfEntries - 1;

    long start = now();
    loadOwnerKeyRange(startId, stopId, false);
    partitionStart = startId;
    partitionEnd = stopId;
    log.info("Partition allocated to this thread is " + partitionStart + "-" + partitionEnd);

    long count = stopId - startId;
    double time = since(start) / 1000.0;
    log.info(String.format("L2 Cache Warmup complete, %d reads, %.1f seconds, %.1f reads/sec", count, time, count
                           / time));
  }

  @Override
  public void doL1WarmUp() {
    log.info(String.format("L1 Cache already warmup during L2 warmup"));
  }
  @Override
  public void logMisses(boolean logMisses) {
    clinic.setLogMisses(logMisses);
  }

  protected static long now() {
    return System.currentTimeMillis();
  }

  protected static long since(long time) {
    return System.currentTimeMillis() - time;
  }

  @Override
  public void setClinic(final Clinic clinic) {
    this.clinic = clinic;
  }

  @Override
  public void setMaxKeyValue(final int maxKeyValue) {
    this.maxKeyValue = maxKeyValue;
  }

  @Override
  public void setThreadNum(final int threadNum) {
    this.threadNum = threadNum;
  }
}
