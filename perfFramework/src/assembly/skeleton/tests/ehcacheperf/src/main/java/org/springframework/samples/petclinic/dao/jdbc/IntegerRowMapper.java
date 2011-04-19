package org.springframework.samples.petclinic.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * @author Alex Snaps
 */
class IntegerRowMapper implements ParameterizedRowMapper<Integer> {

  public Integer mapRow(final ResultSet resultSet, final int i) throws SQLException {
    int value = resultSet.getInt(1);
    return resultSet.wasNull() ? null : value;
  }
}
