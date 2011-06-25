package com.terracotta.cache;

import org.springframework.samples.petclinic.CacheEntryAdapter;

import com.googlecode.hibernate.memcached.Memcache;
import com.googlecode.hibernate.memcached.MemcacheClientFactory;
import com.googlecode.hibernate.memcached.PropertiesHelper;
import com.googlecode.hibernate.memcached.spymemcached.SpyMemcacheClientFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MemcacheWrapper<K, V> extends AbstractCacheWrapper<K, V> {

  private final String                 cacheName;
  private Memcache                     memcache            = null;
  private final static String          MEMCACHE_PROPS_FILE = "memcache-props.properties";
  private static MemcacheClientFactory clientFactory;

  static {
    Properties props = loadProperties(MEMCACHE_PROPS_FILE);
    PropertiesHelper propsHelper = new PropertiesHelper(props);
    clientFactory = new SpyMemcacheClientFactory(propsHelper);
  }

  public MemcacheWrapper(final String cacheName) {
    this.cacheName = cacheName;
    try {
      this.memcache = clientFactory.createMemcacheClient();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void putInCache(final K key, final V value, final CacheEntryAdapter<V> adapter) {
    memcache.set(cacheName + key, 0, value);
  }

  @Override
  public V getFromCache(final K key, CacheEntryAdapter<V> adapter) {
    return (V) memcache.get(cacheName + key);
  }

  public Object getUnderlyingCache() {
    return this;
  }

  private static Properties loadProperties(final String location) {
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return props;
  }

  public int getSize() {
    throw new UnsupportedOperationException();
  }
}
