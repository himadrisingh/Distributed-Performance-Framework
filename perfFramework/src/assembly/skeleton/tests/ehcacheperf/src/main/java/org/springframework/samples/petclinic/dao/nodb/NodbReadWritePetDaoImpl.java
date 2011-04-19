package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.Pet;

public class NodbReadWritePetDaoImpl extends NodbPetDaoImpl {

  protected void update(final Pet value, final String insertStatementParam) {
    // drop write to db
  }

  protected void insert(final Pet value, final String updateStatementParam) {
    // drop write to db
  }

}
