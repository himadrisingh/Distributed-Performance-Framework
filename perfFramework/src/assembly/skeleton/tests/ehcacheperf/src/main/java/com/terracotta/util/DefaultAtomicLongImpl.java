/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.util;

import org.terracotta.util.ClusteredAtomicLong;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultAtomicLongImpl implements ClusteredAtomicLong {
  
  private AtomicLong  counter = new AtomicLong();

  public long addAndGet(long arg0) {
    return counter.addAndGet(arg0);
  }

  public byte byteValue() {
    return counter.byteValue();
  }

  public boolean compareAndSet(long arg0, long arg1) {
    return counter.compareAndSet(arg0, arg1);
  }

  public long decrementAndGet() {
    return counter.decrementAndGet();
  }

  public double doubleValue() {
    return counter.doubleValue();
  }

  public float floatValue() {
    return counter.floatValue();
  }

  public long get() {
    return counter.get();
  }

  public long getAndAdd(long arg0) {
    return counter.getAndAdd(arg0) ;
  }

  public long getAndDecrement() {
    return counter.getAndDecrement();
  }

  public long getAndIncrement() {
    return counter.getAndIncrement();
  }

  public long getAndSet(long arg0) {
    return counter.getAndSet(arg0);
  }

  public long incrementAndGet() {
    return counter.incrementAndGet();
  }

  public int intValue() {
    return counter.intValue();
  }

  public long longValue() {
    return counter.longValue();
  }

  public void set(long arg0) {
    counter.set(arg0);
  }

  public short shortValue() {
    return counter.shortValue();
  }

  public boolean weakCompareAndSet(long arg0, long arg1) {
    return counter.weakCompareAndSet(arg0, arg1);
  }

}
