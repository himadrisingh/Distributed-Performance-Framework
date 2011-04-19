package org.springframework.samples.petclinic.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.dao.OwnerDao;

/**
 * @author Alex Snaps
 */
public class OwnerDaoImpl extends GenericDao<Integer, Owner> implements OwnerDao {

  private final String findByLastName;
  private final String getByAccount;

  public OwnerDaoImpl() {
    super("owners");
    findByLastName = "select * from " + tableName + " where last_name = ?";
    getByAccount = "select * from " + tableName + " where account = ?";

    insertStatement = "insert into " + tableName
                      + " values (default, :account, :firstName, :lastName, :address, :city, :telephone)";
    updateStatement = "update "
                      + tableName
                      + " set account = :account, first_name = :firstName, last_name = :lastName, address = :address, city = :city, telephone = :telephone where id = :id";

    mapper = new OwnerRowMapper();
    adapter = (CacheEntryAdapter<Owner>) mapper;
  }

  public Collection<Owner> findByLastName(final String lastName) {
    return jdbcTemplate.query(findByLastName, mapper, lastName);
  }

  public Owner getByAccount(final Integer account) {
    return jdbcTemplate.queryForObject(getByAccount, mapper, account);
  }

  private static class OwnerRowMapper implements ParameterizedRowMapper<Owner>, CacheEntryAdapter<Owner> {

    public Owner mapRow(final ResultSet resultSet, final int i) throws SQLException {
      Owner owner = new Owner();
      int col = 0;
      owner.setId(resultSet.getInt(++col));
      owner.setAccount(resultSet.getInt(++col));
      if (resultSet.wasNull()) {
        owner.setAccount(null);
      }
      owner.setFirstName(resultSet.getString(++col));
      owner.setLastName(resultSet.getString(++col));
      owner.setAddress(resultSet.getString(++col));
      owner.setCity(resultSet.getString(++col));
      owner.setTelephone(resultSet.getString(++col));
      return owner;
    }

    public Owner hydrate(final Object[] data) {
      Owner owner = new Owner();
      int index = -1;
      owner.setId((Integer) data[++index]);
      owner.setAccount((Integer) data[++index]);
      owner.setFirstName((String) data[++index]);
      owner.setLastName((String) data[++index]);
      owner.setAddress((String) data[++index]);
      owner.setCity((String) data[++index]);
      owner.setTelephone((String) data[++index]);
      return owner;
    }

    public Object[] dehydrate(final Owner value) {
      Object[] data = new Object[7];
      int index = -1;
      data[++index] = value.getId();
      data[++index] = value.getAccount();
      data[++index] = value.getFirstName();
      data[++index] = value.getLastName();
      data[++index] = value.getAddress();
      data[++index] = value.getCity();
      data[++index] = value.getTelephone();
      return data;
    }
  }
}
