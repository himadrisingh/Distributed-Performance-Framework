/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.terracotta.ehcache.perf.test.hotset;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author abhi.sanoujam
 */
public class ReadMostlyHotSetTest extends HotSetDataTest {

  private static final Logger log             = Logger.getLogger(ReadMostlyHotSetTest.class);
  public static final String  PERFTEST        = "PERFTEST";
  private static final AtomicInteger  visits     = new AtomicInteger();

  @Override
  public void doTestBody() {
    int key = nextReadAccountNumberByDistribution();
    Owner owner = clinic.getOwner(key);
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
}
