package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.Visit;

public class NodbReadWriteVisitDaoImpl extends NodbVisitDaoImpl {

  protected void update(final Visit value, final String insertStatementParam) {
    // no db
  }

  protected void insert(final Visit value, final String updateStatementParam) {
    // no db
  }

}
