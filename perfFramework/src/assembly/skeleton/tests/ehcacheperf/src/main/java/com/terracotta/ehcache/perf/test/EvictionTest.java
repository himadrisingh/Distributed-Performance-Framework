package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.store.chm.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class EvictionTest extends AbstractTest {

  private static final int                   HOT_SET_SIZE    = 30000;
  private static final Logger                log             = Logger.getLogger(ReadWriteTest.class);
  public static final String                 PERFTEST        = "PERFTEST";
  private static final AtomicInteger  visits     = new AtomicInteger();

  private final ConcurrentMap<Long, Integer> offsetMap       = new ConcurrentHashMap<Long, Integer>();

  private final AtomicInteger                currentOffset   = new AtomicInteger(0);

  private final Timer                        offsetTimer     = new Timer("offset_timer");

  private final ThreadLocal<Random>          randomLocal     = new ThreadLocal<Random>() {
                                                               @Override
                                                               public Random initialValue() {
                                                                 return new Random();
                                                               }
                                                             };

  @Override
  public void doL1WarmUp() {
    // no-op
  }

  @Override
  public void beforeTest() {
    super.beforeTest();
    OffsetTimerTask offsetTimerTask = new OffsetTimerTask(currentOffset, offsetMap);
    offsetTimer.schedule(offsetTimerTask, 60 * 1000, 10 * 1000);
  }

  private static final class OffsetTimerTask extends TimerTask {

    private static final int                   INCREMENT = 1000;
    private final AtomicInteger                currentOffset;
    private final ConcurrentMap<Long, Integer> offsetMap;

    public OffsetTimerTask(AtomicInteger currentOffset, ConcurrentMap<Long, Integer> offsetMap) {
      this.currentOffset = currentOffset;
      this.offsetMap = offsetMap;
    }

    @Override
    public void run() {
      int cOffset = currentOffset.getAndAdd(INCREMENT);
      log.info("UPDATED offset: " + cOffset);
      for (Long tid : offsetMap.keySet()) {
        offsetMap.put(tid, cOffset);
      }
    }

  }

  @Override
  public void doTestBody() {
    int key = getNextKey();
    Owner owner = clinic.getOwner(key + 1);
    if (owner == null) { throw new RuntimeException("Key '" + key + "' has no value in the cache"); }
    if (doWrite()) {
      // This action now mutates the Owner and adds a visit.
      owner.setTelephone(random.nextInt(999999999) + "");
      List<Pet> pets = owner.getPets();
      // add a visit
      if (pets.size() > 0) {
        Pet pet = pets.get(0);
        Visit visit = new Visit(PERFTEST);
        int id = (Integer.MAX_VALUE / numberOfNodes * nodeId)  + visits.incrementAndGet();
        visit.setId(id);
        pet.addVisit(visit);
      } else {
        log.error("owner with no pets: " + owner.getAccount());
      }
      clinic.storeOwner(owner);
      clinic.refreshCache(owner);
    }
  }

  private int getNextKey() {
    Random randomloc = randomLocal.get();
    int key = randomloc.nextInt(HOT_SET_SIZE);
    Integer offset = offsetMap.get(Thread.currentThread().getId());
    if (offset == null) {
      Integer cOffset = currentOffset.get();
      offsetMap.put(Thread.currentThread().getId(), cOffset);
      offset = cOffset;
    }
    return key + offset;
  }

}
