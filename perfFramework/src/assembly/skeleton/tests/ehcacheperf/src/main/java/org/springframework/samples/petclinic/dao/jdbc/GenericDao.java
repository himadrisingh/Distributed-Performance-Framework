package org.springframework.samples.petclinic.dao.jdbc;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.samples.petclinic.BaseEntity;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.dao.Dao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

/**
 * @author Alex Snaps
 */
public abstract class GenericDao<K, V extends BaseEntity> implements Dao<K, V> {

  private static final Logger         log = Logger.getLogger(GenericDao.class);

  protected final String              tableName;
  private final String                findById;
  private final String                findAll;
  protected String                    insertStatement;
  protected String                    updateStatement;

  protected DataSource                datasource;
  protected SimpleJdbcTemplate        jdbcTemplate;
  protected ParameterizedRowMapper<V> mapper;
  protected CacheEntryAdapter<V>      adapter;
  protected CacheWrapper<K, V>        cache;
  private volatile boolean            logMisses;

  public GenericDao(final String tableName) {
    this.tableName = tableName;
    findAll = "select * from " + tableName;
    findById = findAll + " where id = ?";
  }

  public V getById(final K id) {

    V value;
    if ((value = cache.get(id, adapter)) == null) {
      if (logMisses) {
        log.warn("Getting data from DB because of miss on " + tableName + "#" + id);
      }
      value = this.jdbcTemplate.queryForObject(findById, mapper, id);
      if (value != null) {
        cache.put(id, value, adapter);
      }
    }
    return value;
  }

  public V loadById(final K id) {

    V value;
    value = this.jdbcTemplate.queryForObject(findById, mapper, id);
    if (value != null) {
      cache.put(id, value, adapter);
    }
    return value;
  }

  public List<V> findAll() {
    return this.jdbcTemplate.query(findAll, mapper);
  }

  protected void update(final V value, final String insertStatementParam) {
    SqlParameterSource fileParameters = new BeanPropertySqlParameterSource(value);
    new NamedParameterJdbcTemplate(datasource).update(insertStatementParam, fileParameters);
  }

  protected void insert(final V value, final String updateStatementParam) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    SqlParameterSource fileParameters = new BeanPropertySqlParameterSource(value);
    new NamedParameterJdbcTemplate(datasource).update(updateStatementParam, fileParameters, keyHolder);
    value.setId(keyHolder.getKey().intValue());
  }

  public void store(final V value) {

    if (value.getId() == null) {
      insert(value, insertStatement);
    } else {
      update(value, updateStatement);
    }
  }

  public void setJdbcTemplate(final SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void setCache(final CacheWrapper<K, V> cache) {
    this.cache = cache;
  }

  public void setDatasource(final DataSource datasource) {
    this.datasource = datasource;
  }

  public void putInCache(V value) {
    cache.put((K) value.getId(), value, adapter);
  }

  public void putInCache(Object owner, Collection<V> collection) {
    throw new UnsupportedOperationException();
  }

  public void setLogMisses(final boolean logMisses) {
    this.logMisses = logMisses;
  }

  /**
   * processes all associated caches associated with the input {@link CacheProcessor}
   * 
   * @param cacheProcessor
   */
  public void processAssociatedCaches(CacheProcessor cacheProcessor) {
    if (cacheProcessor == null) return;
    cacheProcessor.processCache(cache);
  }
  
  public int getSize() {
    return cache.getSize();
  }
}
