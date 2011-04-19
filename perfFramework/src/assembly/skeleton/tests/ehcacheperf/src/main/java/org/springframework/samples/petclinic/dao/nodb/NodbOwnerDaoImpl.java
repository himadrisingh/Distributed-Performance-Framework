package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.dao.OwnerDao;

import java.util.Collection;

public class NodbOwnerDaoImpl extends BaseNoDBDAOImpl<Integer, Owner> implements OwnerDao {
  // 2MM owners
  // private static final Integer OwnerLastNames = 2000;
  private static final Integer OwnerFirstNames = 1000;

  public NodbOwnerDaoImpl() {
    super();
  }

  public Collection<Owner> findByLastName(final String lastName) {
    throw new UnsupportedOperationException("findByLastName");
  }

  public Owner getById(final Integer id) {
    Owner value;
    if ((value = cache.get(generateKey(id), adapter)) == null) {
      value = getOwner(id);
      if (value != null) {
        cache.put(generateKey(id), value, adapter);
      }
    }
    return value;
  }

  public Owner loadById(final Integer id) {
    Owner value;
    value = getOwner(id);
    if (value != null) {
      cache.put(generateKey(id), value, adapter);
    }
    return value;
  }

  public Owner getByAccount(final Integer account) {
    return getOwner(account);
  }

  private Owner getOwner(final Integer account) {
    Owner owner = new Owner();
    int firstname = account % OwnerFirstNames;
    int lastname = account / OwnerFirstNames;
    owner.setId(account);
    owner.setAccount(account);
    owner.setFirstName("Owner_First_Name_" + firstname);
    owner.setLastName("Owner_Last_Name_" + lastname);
    owner.setAddress("Street Number " + firstname + getPaddingString(valuePaddingInBytes));
    owner.setCity("City Name " + lastname);
    owner.setTelephone("" + (account + 6085550001L));
    return owner;
  }

  private static class OwnerRowMapper implements CacheEntryAdapter<Owner> {

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

  @Override
  public void setCacheEntryAdapter() {
    // if copyOnRead is enabled, its thread-safe even without dehydrating
    this.adapter = (copyOnRead) ? null : new OwnerRowMapper();
  }

}
