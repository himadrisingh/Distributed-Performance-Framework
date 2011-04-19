package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.PetType;

public class NodbReadWritePetTypeDaoImpl extends NodbPetTypeDaoImpl {

  protected void update(final PetType value, final String insertStatementParam) {
    // drop write to db
  }

  protected void insert(final PetType value, final String updateStatementParam) {
    // drop write to db
  }

}
