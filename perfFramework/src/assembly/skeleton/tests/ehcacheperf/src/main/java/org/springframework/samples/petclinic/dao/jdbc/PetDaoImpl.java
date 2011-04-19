package org.springframework.samples.petclinic.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.dao.PetDao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

/**
 * @author Alex Snaps
 */
public class PetDaoImpl extends GenericDao<Integer, Pet> implements PetDao {

  private CacheWrapper<Integer, List<Integer>> ownerPetCache;

  public PetDaoImpl() {
    super("pets");

    insertStatement = "insert into " + tableName + " values (default, :name, :birthDate, :typeFK, :ownerFK)";
    updateStatement = "update "
                      + tableName
                      + " set name = :name, birth_date = :birthDate, type_id = :typeFK, owner_id = :ownerFK where id = :id";

    mapper = new PetRowMapper();
    adapter = (CacheEntryAdapter<Pet>) mapper;
  }

  @Override
  public void processAssociatedCaches(CacheProcessor cacheProcessor) {
    super.processAssociatedCaches(cacheProcessor);
    cacheProcessor.processCache(ownerPetCache);
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
    return jdbcTemplate.query("select id from pets where owner_id = ?", new IntegerRowMapper(), owner.getId());
  }

  @Override
  public void putInCache(final Object owner, final Collection<Pet> pets) {
    List<Integer> petIdList = new ArrayList<Integer>();
    for (Pet pet : pets) {
      petIdList.add(pet.getId());
    }
    ownerPetCache.put(((Owner) owner).getId(), petIdList, null);
  }

  public void setOwnerPetCache(final CacheWrapper<Integer, List<Integer>> ownerPetCache) {
    this.ownerPetCache = ownerPetCache;
  }

  private static class PetRowMapper implements ParameterizedRowMapper<Pet>, CacheEntryAdapter<Pet> {

    public Pet mapRow(final ResultSet resultSet, final int i) throws SQLException {
      Pet pet = new Pet();
      int column = 0;
      pet.setId(resultSet.getInt(++column));
      pet.setName(resultSet.getString(++column));
      pet.setBirthDate(resultSet.getDate(++column));
      pet.setTypeFK(resultSet.getInt(++column));
      pet.setOwnerFK(resultSet.getInt(++column));
      return pet;
    }

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
}
