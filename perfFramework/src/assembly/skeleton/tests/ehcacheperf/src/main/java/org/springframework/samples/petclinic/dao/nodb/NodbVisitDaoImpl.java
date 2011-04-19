package org.springframework.samples.petclinic.dao.nodb;

import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.samples.petclinic.dao.VisitDao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class NodbVisitDaoImpl extends BaseNoDBDAOImpl<Integer, Visit> implements VisitDao {
  private static final int                     NO_OF_PROCEDURES = 20;
  private final Random                         rand;
  private final ThreadLocal<SimpleDateFormat>  formatter        = new ThreadLocal<SimpleDateFormat>() {

    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd");
    }

  };

  private CacheWrapper<Integer, List<Integer>> petVisitCache;

  public NodbVisitDaoImpl() {
    super();
    this.rand = new Random();
  }

  protected List<Integer> retrieveVisitIdList(final Pet pet) {
    // every pet has two visits
    Integer id = pet.getId() * 2;
    List<Integer> visitIdList = new ArrayList<Integer>();
    visitIdList.add(id);
    visitIdList.add(id + 1);
    return visitIdList;
  }

  public Visit getById(final Integer id) {
    Visit value;
    if ((value = cache.get(generateKey(id), adapter)) == null) {
      value = getVisit(id);
      if (value != null) {
        cache.put(generateKey(id), value, adapter);
      }
    }
    return value;
  }

  public Visit loadById(final Integer id) {
    Visit value;
    value = getVisit(id);
    if (value != null) {
      cache.put(generateKey(id), value, adapter);
    }
    return value;
  }

  private Visit getVisit(final Integer id) {
    Visit visit = new Visit();
    visit.setId(id);
    try {
      visit.setDate(formatter.get().parse(getRandomDate()));
      visit.setDescription(getPaddingString(valuePaddingInBytes));
    } catch (ParseException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    int procedure = rand.nextInt(NO_OF_PROCEDURES) + 1;
    visit.setDescription("PROCEDURE_" + procedure);
    return visit;
  }

  private String getRandomDate() {
    int day = rand.nextInt(27) + 1;
    int month = rand.nextInt(12) + 1;
    int year = rand.nextInt(4) + 2005;
    return String.format("%4d-%02d-%02d", year, month, day);
  }

  private static class VisitRowMapper implements CacheEntryAdapter<Visit> {

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

  @Override
  public void setCacheEntryAdapter() {
    // if copyOnRead is enabled, its thread-safe even without dehydrating
    this.adapter = (copyOnRead) ? null : new VisitRowMapper();
  }

}
