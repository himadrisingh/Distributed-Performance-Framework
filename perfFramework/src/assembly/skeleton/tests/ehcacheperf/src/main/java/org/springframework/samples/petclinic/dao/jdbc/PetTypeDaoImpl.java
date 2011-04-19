package org.springframework.samples.petclinic.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.PetType;
import org.springframework.samples.petclinic.dao.PetTypeDao;

/**
 * @author Alex Snaps
 */
public class PetTypeDaoImpl extends GenericDao<Integer, PetType> implements PetTypeDao {
  public PetTypeDaoImpl() {
    super("types");
    mapper = new PetTypeRowMapper();
    adapter = (CacheEntryAdapter<PetType>) mapper;
  }

  private static class PetTypeRowMapper implements ParameterizedRowMapper<PetType>, CacheEntryAdapter<PetType> {

    public PetType mapRow(final ResultSet resultSet, final int i) throws SQLException {
      PetType type = new PetType();
      int column = 0;
      type.setId(resultSet.getInt(++column));
      type.setName(resultSet.getString(++column));
      return type;
    }

    public PetType hydrate(final Object[] data) {
      PetType petType = new PetType();
      int index = -1;
      petType.setId((Integer) data[++index]);
      petType.setName((String) data[++index]);
      return petType;
    }

    public Object[] dehydrate(final PetType value) {
      Object[] data = new Object[2];
      int index = -1;
      data[++index] = value.getId();
      data[++index] = value.getName();
      return data;
    }
  }
}
