package org.springframework.samples.petclinic.dao;

import java.util.Collection;
import java.util.List;

import com.terracotta.cache.CacheProcessor;

/**
 * @author Alex Snaps
 */
public interface Dao<K, V> {

  /**
   * Loads from data source (if not already cached) and caches result. Always checks cache before operation. If already
   * cached, returns cached result
   * 
   * @param id
   * @return
   */
  V getById(K id);

  /**
   * Loads from data source and caches result. Does not check cache before operation
   * 
   * @param id
   * @return
   */
  V loadById(K id);

  List<V> findAll();

  void store(V value);

  void putInCache(V value);

  void putInCache(Object owner, Collection<V> collection);

  /**
   * processes all associated caches associated with the input {@link CacheProcessor}
   * 
   * @param cacheProcessor
   */
  void processAssociatedCaches(CacheProcessor cacheProcessor);
  
  int getSize();
}
