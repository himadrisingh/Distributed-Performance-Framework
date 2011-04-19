/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.perf.test;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MovingKeySetTest extends AbstractTest {
  private static final Logger        log              = Logger.getLogger(MovingKeySetTest.class);
  public static final String         PERFTEST         = "PERFTEST";
  private static final AtomicInteger visits           = new AtomicInteger();

  private int                        movingKeySetTime = 60 * 1000;
  private int                        moveKeySetNumber = 10000;
  private volatile long              lastMoveTime     = now();
  private volatile int               start            = 0;
  private volatile int               startKey         = maxKeyValue;

  @Override
  public void doTestBody() {
    if (now() - lastMoveTime >= movingKeySetTime) {
      synchronized (this) {
        // check it again so that two threads don't increase it
        if (now() - lastMoveTime >= movingKeySetTime) {
          lastMoveTime = now();
          start += moveKeySetNumber;
          startKey += moveKeySetNumber;
        }
      }
    }

    int key = start + this.random.nextInt(maxKeyValue);
    Owner owner = clinic.getOwner(key);
    if (owner == null && key > startKey) {
      clinic.loadOwner(key);
    } else {
      if (doWrite()) {
        // This action now mutates the Owner and adds a visit.
        owner.setTelephone(random.nextInt(999999999) + "");
        List<Pet> pets = owner.getPets();
        // add a visit
        if (pets.size() > 0) {
          Pet pet = pets.get(0);
          Visit visit = new Visit(PERFTEST);
          int id = (Integer.MAX_VALUE / numberOfNodes * nodeId) + visits.incrementAndGet();
          visit.setId(id);
          pet.addVisit(visit);
        } else {
          log.error("owner with no pets: " + owner.getAccount());
        }
        clinic.storeOwner(owner);
        clinic.refreshCache(owner);
      }
    }
  }

  public void setMovingKeySetTime(int movingKeySetTime) {
    this.movingKeySetTime = movingKeySetTime;
  }

  public void setMoveKeySetNumber(int moveKeySetNumber) {
    this.moveKeySetNumber = moveKeySetNumber;
  }

  @Override
  public void setMaxKeyValue(int maxKeyValue) {
    super.setMaxKeyValue(maxKeyValue);
    this.startKey = maxKeyValue;
  }

}
