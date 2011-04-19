package org.springframework.samples.petclinic;

import net.sf.ehcache.CacheManager;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.dao.OwnerDao;
import org.springframework.samples.petclinic.dao.PetDao;
import org.springframework.samples.petclinic.dao.PetTypeDao;
import org.springframework.samples.petclinic.dao.VisitDao;
import org.springframework.samples.petclinic.dao.jdbc.GenericDao;
import org.springframework.transaction.annotation.Transactional;

import com.terracotta.cache.CacheProcessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Alex Snaps
 */
@Transactional
public class ClinicJdbcImpl implements Clinic {

  private CacheManager cacheManager;

  private PetTypeDao petTypeDao;
  private VisitDao   visitDao;
  private PetDao     petDao;
  private OwnerDao   ownerDao;

  public Collection<PetType> getPetTypes() throws DataAccessException {
    return petTypeDao.findAll();
  }

  public Collection<Owner> findOwners(final String lastName) throws DataAccessException {
    return ownerDao.findByLastName(lastName);
  }

  public Owner findOwnerByAccount(final Integer account) throws DataAccessException {
    return ownerDao.getByAccount(account);
  }

  public Owner getOwner(final int id) throws DataAccessException {
    Owner owner = ownerDao.getById(id);
    populatePets(owner);
    for (Pet pet : owner.getPetsInternal()) {
      populateVisits(pet);
      populateType(pet);
    }
    return owner;
  }

  public Owner loadOwner(final int id) throws DataAccessException {
    Owner owner = ownerDao.loadById(id);
    loadPets(owner);
    for (Pet pet : owner.getPetsInternal()) {
      loadVisits(pet);
      loadType(pet);
    }
    return owner;
  }

  private void populateType(final Pet pet) {
    int fk = pet.getTypeFK();
    PetType type = petTypeDao.getById(fk);
    pet.setType(type);
  }

  private void loadType(final Pet pet) {
    int fk = pet.getTypeFK();
    PetType type = petTypeDao.loadById(fk);
    pet.setType(type);
  }

  private void populateVisits(final Pet pet) {
    Collection<Visit> visits = visitDao.getVisitForPet(pet);
    HashSet<Visit> visitSet = new HashSet<Visit>(visits.size());
    for (Visit visit : visits) {
      visit.setPet(pet);
      visitSet.add(visit);
    }
    pet.setVisitsInternal(visitSet);
  }

  private void loadVisits(final Pet pet) {
    Collection<Visit> visits = visitDao.loadVisitForPet(pet);
    HashSet<Visit> visitSet = new HashSet<Visit>(visits.size());
    for (Visit visit : visits) {
      visit.setPet(pet);
      visitSet.add(visit);
    }
    pet.setVisitsInternal(visitSet);
  }

  private void populatePets(final Owner owner) {
    Collection<Pet> pets = petDao.getPetsForOwner(owner);
    Set<Pet> petSet = new HashSet<Pet>(pets.size());
    for (Pet pet : pets) {
      pet.setOwner(owner);
      petSet.add(pet);
    }
    owner.setPetsInternal(petSet);
  }

  private void loadPets(final Owner owner) {
    Collection<Pet> pets = petDao.loadPetsForOwner(owner);
    Set<Pet> petSet = new HashSet<Pet>(pets.size());
    for (Pet pet : pets) {
      pet.setOwner(owner);
      petSet.add(pet);
    }
    owner.setPetsInternal(petSet);
  }

  public void storeOwner(final Owner owner) throws DataAccessException {
    ownerDao.store(owner);
    for (Pet pet : owner.getPetsInternal()) {
      petDao.store(pet);
      for (Visit visit : pet.getVisitsInternal()) {
        visitDao.store(visit);
      }
    }
  }

  public void refreshCache(final Owner owner) {
    ownerDao.putInCache(owner);
    petDao.putInCache(owner, owner.getPetsInternal());
    for (Pet pet : owner.getPetsInternal()) {
      petDao.putInCache(pet);
      visitDao.putInCache(pet, pet.getVisitsInternal());
      for (Visit visit : pet.getVisitsInternal()) {
        visitDao.putInCache(visit);
      }
    }
  }

  public void processAllCaches(CacheProcessor cacheProcessor) {
    ownerDao.processAssociatedCaches(cacheProcessor);
    petDao.processAssociatedCaches(cacheProcessor);
    visitDao.processAssociatedCaches(cacheProcessor);
    petTypeDao.processAssociatedCaches(cacheProcessor);
  }

  public void storeVisit(final Visit visit) throws DataAccessException {
    throw new UnsupportedOperationException("Not yet impl.");
  }

  public void clearAll2LCache() {
    throw new UnsupportedOperationException("Not yet impl.");
  }

  public int deleteAllOwners(final String firstName, final String lastName) {
    throw new UnsupportedOperationException("Not yet impl.");
  }

  public void deleteVisitsByDescription(final String description) {
    throw new UnsupportedOperationException("Not yet impl.");
  }

  public void setLogMisses(final boolean logMisses) {
    ((GenericDao) ownerDao).setLogMisses(logMisses);
    ((GenericDao) petDao).setLogMisses(logMisses);
    ((GenericDao) visitDao).setLogMisses(logMisses);
    ((GenericDao) petTypeDao).setLogMisses(logMisses);
  }

  public void setPetTypeDao(final PetTypeDao petTypeDao) {
    this.petTypeDao = petTypeDao;
  }

  public void setPetDao(final PetDao petDao) {
    this.petDao = petDao;
  }

  public void setVisitDao(final VisitDao visitDao) {
    this.visitDao = visitDao;
  }

  public void setOwnerDao(final OwnerDao ownerDao) {
    this.ownerDao = ownerDao;
  }

  public CacheManager getCacheManager() {
    return cacheManager;
  }

  public void setCacheManager(final CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public Map cacheGetSizes() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    map.put("owners", ownerDao.getSize());
    map.put("pets", petDao.getSize());
    map.put("visits", visitDao.getSize());
    map.put("petType", petTypeDao.getSize());
    return map;
  }
}
