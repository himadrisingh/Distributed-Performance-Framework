package org.springframework.samples.petclinic.dao;

import java.util.Collection;

import org.springframework.samples.petclinic.Owner;

/**
 * @author Alex Snaps
 */
public interface OwnerDao extends Dao<Integer, Owner> {

  Collection<Owner> findByLastName(String lastName);

  Owner getByAccount(Integer account);
}
