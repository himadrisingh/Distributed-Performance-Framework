package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.Owner;

public class NodbReadWriteOwnerDaoImpl extends NodbOwnerDaoImpl {

  protected void update(final Owner value, final String insertStatementParam) {
    // drop write to db
  }

  protected void insert(final Owner value, final String updateStatementParam) {
    // drop write to db
  }

}
