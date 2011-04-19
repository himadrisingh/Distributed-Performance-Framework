package com.terracotta.util;

import org.terracotta.coordination.Barrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Alex Snaps
 */
public class DefaultBarrierImpl implements Barrier {

  private final CyclicBarrier barrier;
  
  public DefaultBarrierImpl(int parties) {
    barrier = new CyclicBarrier(parties);
  }

  public int await() throws InterruptedException, BrokenBarrierException {
    return barrier.await();
  }

  public int await(long arg0) throws InterruptedException, TimeoutException, BrokenBarrierException {
    return barrier.await(arg0, TimeUnit.MILLISECONDS);
  }

  public int getParties() {
    return barrier.getParties();
  }

  public boolean isBroken() {
    return barrier.isBroken();
  }

  public void reset() {
    barrier.reset();
  }

}
