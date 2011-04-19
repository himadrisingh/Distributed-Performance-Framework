/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.perf.test;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import com.terracotta.ehcache.perf.Configuration;
import com.terracotta.ehcache.perf.test.hotset.HotSetDataTest;
import com.terracotta.ehcache.perf.test.hotset.SimpleMovingDistribution;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class ReadWriteMovingHotSetTest extends HotSetDataTest {

  private static final Logger log             = Logger.getLogger(ReadWriteTest.class);
  public static final String  PERFTEST        = "PERFTEST";
  private static final AtomicInteger  visits     = new AtomicInteger();

  @Override
  public void beforeTest() {
    super.beforeTest();
    Configuration conf = driver.getConfiguration();
    if (conf.isAddOwnersEnabled()){
      log.info("Setting Simple Moving Distribution w/ increasing key size.");
      setDistribution(SimpleMovingDistribution
                      .getSimpleMovingDistribution( conf.getHotSetConfiguration(),
                                                    currKeyCount));
    }
  }

  @Override
  public void doTestBody() {
    int key = this.random.nextInt((int)currKeyCount.get());
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

}
