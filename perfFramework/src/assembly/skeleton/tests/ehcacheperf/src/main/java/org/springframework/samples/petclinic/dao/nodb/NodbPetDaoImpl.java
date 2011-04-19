package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.dao.PetDao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class NodbPetDaoImpl extends BaseNoDBDAOImpl<Integer, Pet> implements PetDao {
  private final Random                         rand;
  private final ThreadLocal<SimpleDateFormat>  formatter = new ThreadLocal<SimpleDateFormat>() {

    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd");
    }

  };
  private CacheWrapper<Integer, List<Integer>> ownerPetCache;

  public NodbPetDaoImpl() {
    super();
    this.rand = new Random();
  }

  public Collection<Pet> getPetsForOwner(final Owner owner) {

    List<Integer> petIdList;
    if ((petIdList = ownerPetCache.get(owner.getId(), null)) == null) {
      petIdList = retrievePetIdList(owner);
      ownerPetCache.put(owner.getId(), petIdList, null);
    }

    Collection<Pet> petList = new ArrayList<Pet>(petIdList.size());
    for (Integer id : petIdList) {
      petList.add(getById(id));
    }

    return petList;
  }

  public Collection<Pet> loadPetsForOwner(final Owner owner) {
    List<Integer> petIdList;
    petIdList = retrievePetIdList(owner);
    ownerPetCache.put(owner.getId(), petIdList, null);
    Collection<Pet> petList = new ArrayList<Pet>(petIdList.size());
    for (Integer id : petIdList) {
      petList.add(loadById(id));
    }
    return petList;
  }

  protected List<Integer> retrievePetIdList(final Owner owner) {
    // every owner has two pets
    Integer id = owner.getAccount() * 2;
    List<Integer> petIdList = new ArrayList<Integer>();
    petIdList.add(id);
    petIdList.add(id + 1);
    return petIdList;
  }

  public Pet getById(final Integer id) {
    Pet value;
    if ((value = cache.get(generateKey(id), adapter)) == null) {
      value = getPet(id);
      if (value != null) {
        cache.put(generateKey(id), value, adapter);
      }
    }
    return value;
  }

  public Pet loadById(final Integer id) {
    Pet value;
    value = getPet(id);
    if (value != null) {
      cache.put(generateKey(id), value, adapter);
    }
    return value;
  }

  private Pet getPet(int petId) {
    Pet pet = new Pet();
    pet.setId(petId);
    int petType = petId % NodbPetTypeDaoImpl.NUM_PET_TYPES;
    pet.setTypeFK(petType);
    pet.setName("PET_NAME_" + petType);
    try {
      pet.setBirthDate(formatter.get().parse(getRandomDate()));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    pet.setOwnerFK(petId / 2);
    return pet;
  }

  private String getRandomDate() {
    int day = rand.nextInt(27) + 1;
    int month = rand.nextInt(12) + 1;
    int year = rand.nextInt(4) + 2005;
    return String.format("%4d-%02d-%02d", year, month, day);
  }

  private static class PetRowMapper implements CacheEntryAdapter<Pet> {

    public Pet hydrate(final Object[] data) {
      Pet pet = new Pet();
      int index = -1;
      pet.setId((Integer) data[++index]);
      pet.setName((String) data[++index]);
      pet.setBirthDate((Date) data[++index]);
      pet.setTypeFK((Integer) data[++index]);
      pet.setOwnerFK((Integer) data[++index]);
      return pet;
    }

    public Object[] dehydrate(final Pet value) {
      Object[] data = new Object[5];
      int index = -1;
      data[++index] = value.getId();
      data[++index] = value.getName();
      data[++index] = value.getBirthDate();
      data[++index] = value.getTypeFK();
      data[++index] = value.getOwnerFK();
      return data;
    }
  }

  @Override
  public void putInCache(final Object owner, final Collection<Pet> pets) {
    List<Integer> petIdList = new ArrayList<Integer>();
    for (Pet pet : pets) {
      petIdList.add(pet.getId());
    }
    ownerPetCache.put(((Owner) owner).getId(), petIdList, null);
  }

  @Override
  public void processAssociatedCaches(CacheProcessor cacheProcessor) {
    super.processAssociatedCaches(cacheProcessor);
    cacheProcessor.processCache(ownerPetCache);
  }

  public void setOwnerPetCache(final CacheWrapper<Integer, List<Integer>> ownerPetCache) {
    this.ownerPetCache = ownerPetCache;
  }

  @Override
  public void setCacheEntryAdapter() {
    // if copyOnRead is enabled, its thread-safe even without dehydrating
    this.adapter = (copyOnRead) ? null : new PetRowMapper();
  }

}
