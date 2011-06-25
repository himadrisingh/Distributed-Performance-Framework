package com.terracotta.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Visit;

/**
 * @author Alex Snaps
 */
public class EhCacheWrapperWithWriter<K, V> extends EhCacheWrapper<K, V> {
  public EhCacheWrapperWithWriter(final String cacheName, final CacheManager cacheManager) {
    super(cacheName, cacheManager);
  }

  @Override
  public void putInCache(final K key, final V value, final CacheEntryAdapter<V> vCacheEntryAdapter) {
    Element element = getElement(key, value, vCacheEntryAdapter);
    if (value instanceof Owner || value instanceof Visit) {
      getCache().putWithWriter(element);
    } else {
      getCache().put(element);
    }
  }
}
