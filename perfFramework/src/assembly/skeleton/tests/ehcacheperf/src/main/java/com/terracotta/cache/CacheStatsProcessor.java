/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.cache;

import com.terracotta.util.LongStat;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheStatsProcessor implements CacheProcessor {

  private static CacheStatsProcessor _processor = new CacheStatsProcessor();

  private final AtomicInteger read, write;
  private final LongStat readStat, writeStat;

  private CacheStatsProcessor(){
    read = new AtomicInteger();
    write = new AtomicInteger();

    readStat = new LongStat(1024 * 1024);
    writeStat = new LongStat(1024 * 1024);
  }

  public static CacheStatsProcessor getInstance(){
    return _processor;
  }

  public void processCache(CacheWrapper cacheWrapper) {
    if (cacheWrapper instanceof AbstractCacheWrapper){
      AbstractCacheWrapper wrapper = (AbstractCacheWrapper) cacheWrapper;
      read.addAndGet(wrapper.getReadCount());
      write.addAndGet(wrapper.getWriteCount());
      readStat.add(wrapper.getReadStats());
      writeStat.add(wrapper.getWriteStats());
    }
  }

  public int getRead() {
    return read.get();
  }

  public int getWrite() {
    return write.get();
  }

  public LongStat getReadStat() {
    return readStat;
  }

  public LongStat getWriteStat() {
    return writeStat;
  }

  public void reset(){
    read.set(0);
    write.set(0);
    readStat.reset();
    writeStat.reset();
  }

}