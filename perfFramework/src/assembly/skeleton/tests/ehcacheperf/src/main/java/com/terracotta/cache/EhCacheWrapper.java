package com.terracotta.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.samples.petclinic.CacheEntryAdapter;

/**
 * @author Alex Snaps
 */
public class EhCacheWrapper<K, V> implements CacheWrapper<K, V> {

  private final String       cacheName;
  private final CacheManager cacheManager;

  public EhCacheWrapper(final String cacheName, final CacheManager cacheManager) {
    this.cacheName = cacheName;
    this.cacheManager = cacheManager;
  }

  public void put(final K key, final V value, final CacheEntryAdapter<V> adapter) {
    getCache().put(getElement(key, value, adapter));
  }

  protected Element getElement(final K key, final V value, final CacheEntryAdapter<V> adapter) {
    return new Element(key, adapter == null ? value : adapter.dehydrate(value));
  }

  public V get(final K key, CacheEntryAdapter<V> adapter) {
    Element element = getCache().get(key);
    V v = null;
    if (element != null) {
      if (adapter == null) {
        v = (V) element.getValue();
      } else {
        v = adapter.hydrate((Object[]) element.getValue());
      }
    }
    return v;
  }

  public Object getUnderlyingCache() {
    return getCache();
  }

  public Ehcache getCache() {
    return cacheManager.getEhcache(cacheName);
  }
  
  public int getSize() {
    return getCache().getSize();
  }

  /**
   * Replaces the actual cache with the new decoratedCache.
   * <p>
   * The decoratedCache should have same name as the actual cache
   * 
   * @param decoratedCache
   */
  public void replaceCacheWithDecoratedCache(Ehcache decoratedCache) {
    if (!decoratedCache.getName().equals(cacheName)) {
      // sane formatter
      throw new AssertionError("The decorated cache should have same name as the actual cache. Actual cacheName: "
                               + cacheName + " decorate cacheName: " + decoratedCache.getName());
    }
    cacheManager.replaceCacheWithDecoratedCache(cacheManager.getEhcache(cacheName), decoratedCache);
  }
}
