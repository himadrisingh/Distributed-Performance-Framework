/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.springframework.samples.petclinic;

import org.springframework.dao.DataAccessException;

import com.terracotta.cache.CacheProcessor;

import java.util.Collection;
import java.util.Map;

/**
 * The high-level PetClinic business interface.
 * <p>
 * This is basically a data access object. PetClinic doesn't have a dedicated business facade.
 * 
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public interface Clinic {

  /**
   * Retrieve all <code>PetType</code>s from the data store.
   * 
   * @return a <code>Collection</code> of <code>PetType</code>s
   */
  Collection<PetType> getPetTypes() throws DataAccessException; // DO IT

  /**
   * Retrieve <code>Owner</code>s from the data store by last name, returning all owners whose last name <i>starts</i>
   * with the given name.
   * 
   * @param lastName Value to search for
   * @return a <code>Collection</code> of matching <code>Owner</code>s (or an empty <code>Collection</code> if none
   *         found)
   */
  Collection<Owner> findOwners(String lastName) throws DataAccessException; // DO IT

  /**
   * Retrieve <code>Owner</code> from the data store by account,
   * 
   * @param account Value to search for
   * @return a <code>Owner</code> of matching (or an null if none found)
   */
  Owner findOwnerByAccount(Integer account) throws DataAccessException; // DO IT

  /**
   * Retrieve an <code>Owner</code> from the data store by id. Can hit cache if caching enabled. Returns cached result
   * if already cached or retuns one loaded from datasource
   * 
   * @param id the id to search for
   * @return the <code>Owner</code> if found
   * @throws org.springframework.dao.DataRetrievalFailureException if not found
   */
  Owner getOwner(int id) throws DataAccessException; // DO IT

  /**
   * Same as {@link #getOwner(int)} but doesn't check if already loaded in cache (even if caching is enabled)
   * 
   * @param id
   * @return
   * @throws DataAccessException
   */
  Owner loadOwner(int id) throws DataAccessException; // DO IT

  /**
   * Save an <code>Owner</code> to the data store, either inserting or updating it.
   * 
   * @param owner the <code>Owner</code> to save
   * @see BaseEntity#isNew
   */
  void storeOwner(Owner owner) throws DataAccessException; // DO IT

  void refreshCache(final Owner owner);

  /**
   * Calls process on the cacheProcessor for all caches associated with this clinic
   * 
   * @param cacheProcessor
   */
  void processAllCaches(CacheProcessor cacheProcessor);

  /**
   * Save a <code>Visit</code> to the data store, either inserting or updating it.
   * 
   * @param visit the <code>Visit</code> to save
   * @see BaseEntity#isNew
   */
  void storeVisit(Visit visit) throws DataAccessException; // DO IT

  void clearAll2LCache(); // DO IT

  int deleteAllOwners(String firstName, String lastName); // DO IT

  void deleteVisitsByDescription(String description); // DO IT

  void setLogMisses(boolean logMisses);
  
  Map cacheGetSizes();
}
