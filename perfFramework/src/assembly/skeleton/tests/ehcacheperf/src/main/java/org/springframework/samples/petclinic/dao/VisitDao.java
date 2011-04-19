package org.springframework.samples.petclinic.dao;

import java.util.Collection;

import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

/**
 * @author Alex Snaps
 */
public interface VisitDao extends Dao<Integer, Visit> {

  Collection<Visit> getVisitForPet(Pet pet);

  Collection<Visit> loadVisitForPet(Pet pet);
}
