package org.springframework.samples.petclinic.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.samples.petclinic.dao.VisitDao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

/**
 * @author Alex Snaps
 */
public class VisitDaoImpl extends GenericDao<Integer, Visit> implements VisitDao {

  private CacheWrapper<Integer, List<Integer>> petVisitCache;

  public VisitDaoImpl() {
    super("visits");

    insertStatement = "insert into " + tableName + " values (default, :petFK, :date, :description)";
    updateStatement = "update " + tableName
                      + " set pet_id = :petFK, visit_date = :date, description = :description where id = :id";

    mapper = new VisitRowMapper();
    adapter = (CacheEntryAdapter<Visit>) mapper;
  }

  @Override
  public void processAssociatedCaches(CacheProcessor cacheProcessor) {
    super.processAssociatedCaches(cacheProcessor);
    cacheProcessor.processCache(petVisitCache);
  }

  public Collection<Visit> getVisitForPet(final Pet pet) {
    List<Integer> visitIdList;
    if ((visitIdList = petVisitCache.get(pet.getId(), null)) == null) {
      visitIdList = retrieveVisitIdList(pet);
      petVisitCache.put(pet.getId(), visitIdList, null);
    }
    Collection<Visit> visitList = new ArrayList<Visit>(visitIdList.size());
    for (Integer id : visitIdList) {
      visitList.add(getById(id));
    }
    return visitList;
  }

  public Collection<Visit> loadVisitForPet(final Pet pet) {
    List<Integer> visitIdList;
    visitIdList = retrieveVisitIdList(pet);
    petVisitCache.put(pet.getId(), visitIdList, null);
    Collection<Visit> visitList = new ArrayList<Visit>(visitIdList.size());
    for (Integer id : visitIdList) {
      visitList.add(loadById(id));
    }
    return visitList;
  }

  protected List<Integer> retrieveVisitIdList(final Pet pet) {
    return jdbcTemplate.query("select id from visits where pet_id = ?", new IntegerRowMapper(), pet.getId());
  }

  @Override
  public void putInCache(final Object pet, final Collection<Visit> visits) {
    List<Integer> visitIdList = new ArrayList<Integer>();
    for (Visit visit : visits) {
      visitIdList.add(visit.getId());
    }
    petVisitCache.put(((Pet) pet).getId(), visitIdList, null);
  }

  public void setPetVisitCache(final CacheWrapper<Integer, List<Integer>> petVisitCache) {
    this.petVisitCache = petVisitCache;
  }

  private static class VisitRowMapper implements ParameterizedRowMapper<Visit>, CacheEntryAdapter<Visit> {
    public Visit mapRow(final ResultSet resultSet, final int i) throws SQLException {
      Visit visit = new Visit();
      int column = 0;
      visit.setId(resultSet.getInt(++column));
      visit.setPetFK(resultSet.getInt(++column));
      visit.setDate(resultSet.getDate(++column));
      visit.setDescription(resultSet.getString(++column));
      return visit;
    }

    public Visit hydrate(final Object[] data) {
      Visit visit = new Visit();
      int index = -1;
      visit.setId((Integer) data[++index]);
      visit.setPetFK((Integer) data[++index]);
      visit.setDate((Date) data[++index]);
      visit.setDescription((String) data[++index]);
      return visit;
    }

    public Object[] dehydrate(final Visit value) {
      Object[] data = new Object[4];
      int index = -1;
      data[++index] = value.getId();
      data[++index] = value.getPetFK();
      data[++index] = value.getDate();
      data[++index] = value.getDescription();
      return data;
    }
  }
}
