package com.terracotta.cache;

import org.springframework.samples.petclinic.CacheEntryAdapter;

/**
 * @author Alex Snaps
 */
public interface CacheWrapper<K, V> {

  void put(K key, V value, CacheEntryAdapter<V> adapter);

  V get(K key, CacheEntryAdapter<V> adapter);

  Object getUnderlyingCache();
  
  int getSize();
}
