/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.springframework.samples.petclinic.perftest;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Clinic;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.transaction.annotation.Transactional;

// import javax.annotation.Resource;

/**
 * Example performance test item. This class is a Spring managed Controller bean so that it can be exposed as a web
 * service.
 */
public class PetClinicPerfTestActions {

  public static final String    OK       = "ok";
  public static final String    FAIL     = "fail";

  protected static final Logger log      = Logger.getLogger(PetClinicPerfTestActions.class);

  public static final String    PERFTEST = "PERFTEST";

  private Random                random   = new Random();

  protected Clinic              clinic;

  public String findOwners(String lastNamePrefix) {
    debug("findOwners lastNamePrefix=%s", lastNamePrefix);
    Collection<Owner> results = getClinic().findOwners(lastNamePrefix);
    return (results.size() > 0) ? OK : FAIL;
  }

  public String getOwner(int accountNumber) {
    debug("getOwnerByAccount %d", accountNumber);
    Owner owner = getClinic().getOwner(accountNumber);
    debug("owner = %s", owner);
    return OK;
  }

  public String getOwnerById(int id) {
    debug("getOwnerById %d", id);
    Owner owner = getClinic().getOwner(id);
    debug("owner = %s", owner);
    return OK;
  }

  public void cleanupTestData() {
    log.info("Cleaning test data");
    getClinic().deleteVisitsByDescription(PERFTEST);
  }

  @Transactional
  public String updateOwner(int accountNumber) {
    Owner owner = getClinic().getOwner(accountNumber);
    // randomly mutate the telephone number.
    owner.setTelephone(random.nextInt(999999999) + "");
    List<Pet> pets = owner.getPets();
    // add a visit
    if (pets.size() > 0) {
      Pet pet = pets.get(0);
      Visit visit = new Visit(PERFTEST);
      pet.addVisit(visit);
    } else {
      log.error("owner with no pets: " + owner.getAccount());
    }
    getClinic().storeOwner(owner);
    return OK;
  }

  /**
   * Update owners in the specified range. Currently each owner is updated in a separate transaction. That could be
   * changed by adding an @Transactional annotation to this method.
   */
  public void updateOwners(int startingAccountNum, int endingAccountNum) {
    log.info("Updating Owners...");
    long start = System.currentTimeMillis();
    for (int accountNum = startingAccountNum; accountNum < endingAccountNum; accountNum++) {
      updateOwner(accountNum);
    }
    long end = System.currentTimeMillis();
    log.info(String.format("Time to update %d owners = %d ms", (endingAccountNum - startingAccountNum), (end - start)));
  }

  public void clearAll2LCache() {
    log.info("*** Clearing all 2L caches. ***");
    getClinic().clearAll2LCache();
  }

  public void persistOwner(final Owner owner) {
    getClinic().storeOwner(owner);
  }

  public void deleteAllOwnersAbove(final String firstName, final String lastName) {
    log.info("Clearing all owners named " + firstName + " " + lastName);
    int deleted = getClinic().deleteAllOwners(firstName, lastName);
    log.info("Deleted " + deleted + " Owners");
  }

  public Clinic getClinic() {
    return clinic;
  }

  protected void debug(String messageFormat, Object... args) {
    if (log.isDebugEnabled()) {
      log.debug(String.format(messageFormat, args));
    }
  }

}