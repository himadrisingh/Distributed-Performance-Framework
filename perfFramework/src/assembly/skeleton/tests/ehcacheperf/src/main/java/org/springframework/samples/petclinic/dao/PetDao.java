package org.springframework.samples.petclinic.dao;

import java.util.Collection;

import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;

/**
 * @author Alex Snaps
 */
public interface PetDao extends Dao<Integer, Pet> {

  /**
   * Loads and caches result. Returns already cached value if present in cache
   * 
   * @param owner
   * @return
   */
  Collection<Pet> getPetsForOwner(Owner owner);

  /**
   * Loads and caches result. Does not check cache if already present.
   * 
   * @param owner
   * @return
   */
  Collection<Pet> loadPetsForOwner(Owner owner);

}
