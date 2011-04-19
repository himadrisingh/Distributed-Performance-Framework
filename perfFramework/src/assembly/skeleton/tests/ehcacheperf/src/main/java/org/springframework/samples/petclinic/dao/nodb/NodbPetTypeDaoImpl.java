package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.PetType;
import org.springframework.samples.petclinic.dao.PetTypeDao;

public class NodbPetTypeDaoImpl extends BaseNoDBDAOImpl<Integer, PetType> implements PetTypeDao {
  public static final int NUM_PET_TYPES = 6;

  public NodbPetTypeDaoImpl() {
    super();
  }

  private static final PetType[] petTypes = new PetType[NUM_PET_TYPES];
  static {
    for (int i = 0; i < NUM_PET_TYPES; ++i) {
      petTypes[i] = new PetType();
      petTypes[i].setId(i + 1);
    }
    petTypes[0].setName("cat");
    petTypes[1].setName("dog");
    petTypes[2].setName("lizard");
    petTypes[3].setName("snake");
    petTypes[4].setName("bird");
    petTypes[5].setName("hamster");
  }

  public PetType getById(final Integer id) {
    PetType value;
    if ((value = cache.get(generateKey(id), adapter)) == null) {
      value = petTypes[id];
      if (value != null) {
        cache.put(generateKey(id), value, adapter);
      }
    }
    return value;
  }

  public PetType loadById(final Integer id) {
    PetType value;
    if ((value = cache.get(generateKey(id), adapter)) == null) {
      value = petTypes[id];
      if (value != null) {
        cache.put(generateKey(id), value, adapter);
      }
    }
    return value;
  }

  private static class PetTypeRowMapper implements CacheEntryAdapter<PetType> {

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

  @Override
  public void setCacheEntryAdapter() {
    // if copyOnRead is enabled, its thread-safe even without dehydrating
    this.adapter = (copyOnRead) ? null : new PetTypeRowMapper();
  }

}
